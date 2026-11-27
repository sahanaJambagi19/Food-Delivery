package com.fooddelivery.service.impl;

import com.fooddelivery.dto.request.PlaceOrderRequest;
import com.fooddelivery.dto.response.*;
import com.fooddelivery.entity.*;
import com.fooddelivery.exception.*;
import com.fooddelivery.repository.*;
import com.fooddelivery.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepo;
    private final OrderStatusHistoryRepository historyRepo;
    private final CartRepository cartRepo;
    private final AddressRepository addressRepo;
    private final UserRepository userRepo;
    private final RestaurantRepository restaurantRepo;
    private final CouponRepository couponRepo;
    private final CouponUsageRepository couponUsageRepo;
    private final CouponService couponService;
    private final NotificationService notificationService;

    private static final BigDecimal TAX_RATE = new BigDecimal("0.05"); // 5% GST

    @Override
    @Transactional
    public OrderResponse placeOrder(Long userId, PlaceOrderRequest req) {
        // 1. Validate cart
        Cart cart = cartRepo.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Cart is empty"));
        if (cart.getItems().isEmpty())
            throw new BusinessException("Cart is empty");

        Restaurant restaurant = cart.getRestaurant();
        if (restaurant == null || !restaurant.isOpen() || !restaurant.isApproved() || !restaurant.isActive())
            throw new BusinessException("Restaurant is currently not accepting orders");

        // Revalidate availability and refresh prices from the database at checkout.
        for (CartItem cartItem : cart.getItems()) {
            FoodItem foodItem = cartItem.getFoodItem();
            if (!foodItem.isAvailable() || !foodItem.getCategory().isActive()
                    || !foodItem.getRestaurant().getId().equals(restaurant.getId()))
                throw new BusinessException("One or more cart items are no longer available. Please review your cart.");
            cartItem.setUnitPrice(foodItem.getPrice());
        }
        cartRepo.save(cart);

        // 2. Validate address
        Address address = addressRepo.findById(req.getAddressId())
                .orElseThrow(() -> new ResourceNotFoundException("Address", req.getAddressId()));
        if (!address.getUser().getId().equals(userId))
            throw new BusinessException("Address does not belong to this user");

        // 3. Calculate amounts
        BigDecimal subtotal = cart.getTotal();
        if (subtotal.compareTo(restaurant.getMinOrderAmount()) < 0)
            throw new BusinessException("Minimum order amount is ₹" + restaurant.getMinOrderAmount());

        BigDecimal deliveryCharge = restaurant.getDeliveryCharge();
        BigDecimal tax = subtotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal discount = BigDecimal.ZERO;
        Coupon coupon = null;

        // 4. Apply coupon
        if (req.getCouponCode() != null && !req.getCouponCode().isBlank()) {
            coupon = couponService.getCouponByCode(req.getCouponCode());
            CouponValidateResponse cv = couponService.validate(req.getCouponCode(), userId, subtotal);
            discount = cv.getDiscountAmount();
        }

        BigDecimal total = subtotal.add(deliveryCharge).add(tax).subtract(discount)
                .max(BigDecimal.ZERO);

        // 5. Build order
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Order.PaymentMethod paymentMethod;
        try {
            paymentMethod = Order.PaymentMethod.valueOf(req.getPaymentMethod().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Invalid payment method: " + req.getPaymentMethod());
        }

        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .user(user).restaurant(restaurant).address(address).coupon(coupon)
                .status(Order.OrderStatus.PLACED)
                .paymentMethod(paymentMethod)
                .paymentStatus(paymentMethod == Order.PaymentMethod.COD
                        ? Order.PaymentStatus.PENDING : Order.PaymentStatus.PENDING)
                .subtotal(subtotal).deliveryCharge(deliveryCharge)
                .taxAmount(tax).discountAmount(discount).totalAmount(total)
                .specialInstructions(req.getSpecialInstructions())
                .estimatedDeliveryTime(restaurant.getAvgDeliveryTime())
                .build();

        // 6. Add order items from cart
        cart.getItems().forEach(ci -> {
            OrderItem oi = OrderItem.builder()
                    .order(order).foodItem(ci.getFoodItem())
                    .name(ci.getFoodItem().getName())
                    .quantity(ci.getQuantity()).unitPrice(ci.getUnitPrice())
                    .totalPrice(ci.getSubtotal()).build();
            order.getItems().add(oi);
        });

        Order saved = orderRepo.save(order);

        // 7. Record status history
        saveHistory(saved, Order.OrderStatus.PLACED.name(), "Order placed successfully");

        // 8. Record coupon usage
        if (coupon != null) {
            coupon.setUsedCount(coupon.getUsedCount() + 1);
            couponRepo.save(coupon);
            couponUsageRepo.save(CouponUsage.builder()
                    .coupon(coupon).user(user).order(saved).build());
        }

        // 9. Clear cart
        cart.getItems().clear();
        cart.setRestaurant(null);
        cartRepo.save(cart);

        // 10. Notify user and restaurant owner
        notificationService.send(userId, "Order Placed! 🎉",
                "Your order #" + saved.getOrderNumber() + " has been placed.", "ORDER");
        notificationService.send(restaurant.getOwner().getId(), "New Order Received! 🍽️",
                "Order #" + saved.getOrderNumber() + " received. Please confirm.", "ORDER");

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long orderId, Long userId) {
        Order order = findOrder(orderId);
        if (!order.getUser().getId().equals(userId))
            throw new BusinessException("Order does not belong to this user");
        return toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getMyOrders(Long userId, Pageable pageable) {
        return PageResponse.of(orderRepo.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getRestaurantOrders(Long ownerId, Pageable pageable) {
        Restaurant restaurant = restaurantRepo.findByOwnerId(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found for this owner"));
        return PageResponse.of(orderRepo.findByRestaurantIdOrderByCreatedAtDesc(
                restaurant.getId(), pageable).map(this::toResponse));
    }

    @Override
    @Transactional
    public OrderResponse updateStatus(Long ownerId, Long orderId, Order.OrderStatus newStatus, String note) {
        Restaurant restaurant = restaurantRepo.findByOwnerId(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found for this owner"));
        Order order = findOrder(orderId);
        if (!order.getRestaurant().getId().equals(restaurant.getId()))
            throw new BusinessException("Order does not belong to your restaurant");

        Order.OrderStatus current = order.getStatus();
        boolean validTransition =
                (current == Order.OrderStatus.PLACED
                        && (newStatus == Order.OrderStatus.CONFIRMED || newStatus == Order.OrderStatus.REJECTED))
                || (current == Order.OrderStatus.CONFIRMED && newStatus == Order.OrderStatus.PREPARING)
                || (current == Order.OrderStatus.PREPARING && newStatus == Order.OrderStatus.READY_FOR_PICKUP);
        if (!validTransition)
            throw new BusinessException("Invalid restaurant order status transition: " + current + " → " + newStatus);

        if (newStatus == Order.OrderStatus.CONFIRMED
                && order.getPaymentMethod() == Order.PaymentMethod.ONLINE
                && order.getPaymentStatus() != Order.PaymentStatus.PAID)
            throw new BusinessException("Order cannot be confirmed until online payment is successful");
        if (newStatus == Order.OrderStatus.REJECTED
                && order.getPaymentStatus() == Order.PaymentStatus.PAID)
            throw new BusinessException("This order has already been paid. Contact the administrator to process the refund before rejecting it.");

        order.setStatus(newStatus);
        orderRepo.save(order);
        saveHistory(order, newStatus.name(), note);

        notificationService.send(order.getUser().getId(),
                "Order Update 📦", "Your order #" + order.getOrderNumber()
                        + " is now: " + newStatus.name().replace("_", " "), "ORDER");
        return toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getAllOrders(Pageable pageable) {
        return PageResponse.of(orderRepo.findAll(pageable).map(this::toResponse));
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long orderId, Long userId) {
        Order order = findOrder(orderId);
        if (!order.getUser().getId().equals(userId))
            throw new BusinessException("Order does not belong to this user");

        Set<Order.OrderStatus> cancellable = Set.of(
                Order.OrderStatus.PLACED, Order.OrderStatus.CONFIRMED);
        if (!cancellable.contains(order.getStatus()))
            throw new BusinessException("Order cannot be cancelled at this stage");
        if (order.getPaymentStatus() == Order.PaymentStatus.PAID)
            throw new BusinessException("This order has already been paid. Please contact support for cancellation and refund assistance.");

        order.setStatus(Order.OrderStatus.CANCELLED);
        orderRepo.save(order);
        saveHistory(order, Order.OrderStatus.CANCELLED.name(), "Cancelled by customer");

        notificationService.send(userId, "Order Cancelled",
                "Your order #" + order.getOrderNumber() + " has been cancelled.", "ORDER");
        return toResponse(order);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Order findOrder(Long id) {
        return orderRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));
    }

    private void saveHistory(Order order, String status, String note) {
        historyRepo.save(OrderStatusHistory.builder()
                .order(order).status(status).note(note).build());
    }

    private String generateOrderNumber() {
        return "ORD-" + System.currentTimeMillis() + "-"
                + (int)(Math.random() * 9000 + 1000);
    }

    private OrderResponse toResponse(Order o) {
        List<OrderResponse.OrderItemResponse> items = o.getItems().stream()
                .map(i -> OrderResponse.OrderItemResponse.builder()
                        .foodItemId(i.getFoodItem().getId()).name(i.getName())
                        .quantity(i.getQuantity()).unitPrice(i.getUnitPrice())
                        .totalPrice(i.getTotalPrice()).build())
                .collect(Collectors.toList());

        List<OrderResponse.StatusHistoryResponse> history =
                historyRepo.findByOrderIdOrderByChangedAtAsc(o.getId()).stream()
                        .map(h -> OrderResponse.StatusHistoryResponse.builder()
                                .status(h.getStatus()).note(h.getNote())
                                .changedAt(h.getChangedAt()).build())
                        .collect(Collectors.toList());

        Address addr = o.getAddress();
        String fullAddress = addr.getAddressLine() + ", " + addr.getCity()
                + " - " + addr.getPincode();

        return OrderResponse.builder()
                .id(o.getId()).orderNumber(o.getOrderNumber())
                .status(o.getStatus().name()).paymentMethod(o.getPaymentMethod().name())
                .paymentStatus(o.getPaymentStatus().name())
                .restaurantId(o.getRestaurant().getId()).restaurantName(o.getRestaurant().getName())
                .addressId(addr.getId()).deliveryAddress(fullAddress)
                .subtotal(o.getSubtotal()).deliveryCharge(o.getDeliveryCharge())
                .taxAmount(o.getTaxAmount()).discountAmount(o.getDiscountAmount())
                .totalAmount(o.getTotalAmount())
                .couponCode(o.getCoupon() != null ? o.getCoupon().getCode() : null)
                .specialInstructions(o.getSpecialInstructions())
                .estimatedDeliveryTime(o.getEstimatedDeliveryTime())
                .items(items).statusHistory(history).createdAt(o.getCreatedAt())
                .build();
    }
}
