package com.fooddelivery.service.impl;

import com.fooddelivery.dto.request.CartItemRequest;
import com.fooddelivery.dto.response.CartResponse;
import com.fooddelivery.entity.*;
import com.fooddelivery.exception.*;
import com.fooddelivery.repository.*;
import com.fooddelivery.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepo;
    private final FoodItemRepository foodItemRepo;
    private final UserRepository userRepo;

    @Override
    @Transactional
    public CartResponse getCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        return toResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse addItem(Long userId, CartItemRequest req) {
        Cart cart = getOrCreateCart(userId);
        FoodItem foodItem = foodItemRepo.findById(req.getFoodItemId())
                .orElseThrow(() -> new ResourceNotFoundException("FoodItem", req.getFoodItemId()));

        if (!foodItem.isAvailable() || !foodItem.getCategory().isActive())
            throw new BusinessException("This item is currently unavailable");
        Restaurant restaurant = foodItem.getRestaurant();
        if (!restaurant.isActive() || !restaurant.isApproved() || !restaurant.isOpen())
            throw new BusinessException("This restaurant is currently not accepting orders");

        // Cross-restaurant conflict check
        if (cart.getRestaurant() != null &&
                !cart.getRestaurant().getId().equals(foodItem.getRestaurant().getId())) {
            throw new BusinessException(
                    "Your cart has items from another restaurant. Clear cart to add items from this restaurant.");
        }

        cart.setRestaurant(foodItem.getRestaurant());

        // If item already in cart, increment quantity
        CartItem existing = cart.getItems().stream()
                .filter(ci -> ci.getFoodItem().getId().equals(req.getFoodItemId()))
                .findFirst().orElse(null);

        if (existing != null) {
            int newQty = existing.getQuantity() + req.getQuantity();
            if (newQty > 20) throw new BusinessException("Maximum quantity per item is 20");
            existing.setQuantity(newQty);
        } else {
            CartItem item = CartItem.builder()
                    .cart(cart).foodItem(foodItem)
                    .quantity(req.getQuantity()).unitPrice(foodItem.getPrice()).build();
            cart.getItems().add(item);
        }

        return toResponse(cartRepo.save(cart));
    }

    @Override
    @Transactional
    public CartResponse updateItem(Long userId, Long cartItemId, Integer quantity) {
        if (quantity == null || quantity < 0 || quantity > 20)
            throw new BusinessException("Quantity must be between 0 and 20");
        Cart cart = getOrCreateCart(userId);
        CartItem item = findCartItem(cart, cartItemId);
        if (quantity <= 0) {
            cart.getItems().remove(item);
        } else {
            item.setQuantity(quantity);
        }
        if (cart.getItems().isEmpty()) cart.setRestaurant(null);
        return toResponse(cartRepo.save(cart));
    }

    @Override
    @Transactional
    public CartResponse removeItem(Long userId, Long cartItemId) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = findCartItem(cart, cartItemId);
        cart.getItems().remove(item);
        if (cart.getItems().isEmpty()) cart.setRestaurant(null);
        return toResponse(cartRepo.save(cart));
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        cart.getItems().clear();
        cart.setRestaurant(null);
        cartRepo.save(cart);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Cart getOrCreateCart(Long userId) {
        return cartRepo.findByUserId(userId).orElseGet(() -> {
            User user = userRepo.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", userId));
            return cartRepo.save(Cart.builder().user(user).build());
        });
    }

    private CartItem findCartItem(Cart cart, Long cartItemId) {
        return cart.getItems().stream()
                .filter(ci -> ci.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", cartItemId));
    }

    private CartResponse toResponse(Cart cart) {
        List<CartResponse.CartItemResponse> items = cart.getItems().stream()
                .map(ci -> CartResponse.CartItemResponse.builder()
                        .cartItemId(ci.getId())
                        .foodItemId(ci.getFoodItem().getId())
                        .foodItemName(ci.getFoodItem().getName())
                        .imageUrl(ci.getFoodItem().getImageUrl())
                        .veg(ci.getFoodItem().isVeg())
                        .unitPrice(ci.getUnitPrice())
                        .quantity(ci.getQuantity())
                        .subtotal(ci.getSubtotal())
                        .build())
                .collect(Collectors.toList());

        BigDecimal subtotal = items.stream()
                .map(CartResponse.CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .cartId(cart.getId())
                .restaurantId(cart.getRestaurant() != null ? cart.getRestaurant().getId() : null)
                .restaurantName(cart.getRestaurant() != null ? cart.getRestaurant().getName() : null)
                .items(items).subtotal(subtotal)
                .totalItems(items.stream().mapToInt(CartResponse.CartItemResponse::getQuantity).sum())
                .build();
    }
}
