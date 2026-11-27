package com.fooddelivery.service.impl;

import com.fooddelivery.dto.response.DeliveryAssignmentResponse;
import com.fooddelivery.entity.*;
import com.fooddelivery.exception.*;
import com.fooddelivery.repository.*;
import com.fooddelivery.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class DeliveryServiceImpl implements DeliveryService {

    private final DeliveryAssignmentRepository assignmentRepo;
    private final DeliveryPartnerRepository partnerRepo;
    private final OrderRepository orderRepo;
    private final OrderStatusHistoryRepository historyRepo;
    private final NotificationService notificationService;

    private static final BigDecimal DEFAULT_DELIVERY_FEE = new BigDecimal("40.00");

    @Override
    @Transactional
    public DeliveryAssignmentResponse assignDelivery(Long orderId, Long deliveryPartnerId) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        DeliveryAssignment previousAssignment = assignmentRepo.findByOrderId(orderId).orElse(null);
        if (previousAssignment != null
                && previousAssignment.getStatus() != DeliveryAssignment.AssignmentStatus.FAILED)
            throw new DuplicateException("Delivery already assigned for this order");
        if (order.getStatus() != Order.OrderStatus.READY_FOR_PICKUP)
            throw new BusinessException("A delivery can only be assigned after the restaurant marks the order ready for pickup");
        if (order.getPaymentMethod() == Order.PaymentMethod.ONLINE
                && order.getPaymentStatus() != Order.PaymentStatus.PAID)
            throw new BusinessException("Online payment must be confirmed before assigning a delivery partner");

        DeliveryPartner partner = partnerRepo.findById(deliveryPartnerId)
                .orElseThrow(() -> new ResourceNotFoundException("DeliveryPartner", deliveryPartnerId));

        if (!partner.isAvailable())
            throw new BusinessException("Delivery partner is not available");

        DeliveryAssignment assignment;
        if (previousAssignment == null) {
            assignment = DeliveryAssignment.builder()
                    .order(order).deliveryPartner(partner)
                    .status(DeliveryAssignment.AssignmentStatus.ASSIGNED)
                    .deliveryFee(DEFAULT_DELIVERY_FEE).build();
        } else {
            assignment = previousAssignment;
            assignment.setDeliveryPartner(partner);
            assignment.setStatus(DeliveryAssignment.AssignmentStatus.ASSIGNED);
            assignment.setAssignedAt(LocalDateTime.now());
            assignment.setPickedUpAt(null);
            assignment.setDeliveredAt(null);
        }
        DeliveryAssignment saved = assignmentRepo.save(assignment);

        // The order remains READY_FOR_PICKUP until the delivery partner physically picks it up.
        // Reserve this partner so the admin cannot assign another active delivery to them.
        partner.setAvailable(false);
        partnerRepo.save(partner);

        notificationService.send(partner.getUser().getId(), "New Delivery Assigned 🚴",
                "You have a new delivery for order #" + order.getOrderNumber(), "DELIVERY");
        notificationService.send(order.getUser().getId(), "Delivery Partner Assigned 🚴",
                "Your order #" + order.getOrderNumber() + " has been assigned to a delivery partner.", "DELIVERY");

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryAssignmentResponse> getMyAssignments(Long userId) {
        DeliveryPartner partner = findPartnerByUserId(userId);
        return assignmentRepo.findByDeliveryPartnerId(partner.getId())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public DeliveryAssignmentResponse updateAssignmentStatus(Long userId, Long assignmentId,
                                                              DeliveryAssignment.AssignmentStatus newStatus) {
        DeliveryPartner partner = findPartnerByUserId(userId);
        DeliveryAssignment assignment = assignmentRepo.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment", assignmentId));

        if (!assignment.getDeliveryPartner().getId().equals(partner.getId()))
            throw new BusinessException("Assignment does not belong to this delivery partner");

        DeliveryAssignment.AssignmentStatus current = assignment.getStatus();
        boolean validTransition =
                (current == DeliveryAssignment.AssignmentStatus.ASSIGNED
                        && (newStatus == DeliveryAssignment.AssignmentStatus.PICKED_UP
                            || newStatus == DeliveryAssignment.AssignmentStatus.FAILED))
                || (current == DeliveryAssignment.AssignmentStatus.PICKED_UP
                        && (newStatus == DeliveryAssignment.AssignmentStatus.OUT_FOR_DELIVERY
                            || newStatus == DeliveryAssignment.AssignmentStatus.FAILED))
                || (current == DeliveryAssignment.AssignmentStatus.OUT_FOR_DELIVERY
                        && (newStatus == DeliveryAssignment.AssignmentStatus.DELIVERED
                            || newStatus == DeliveryAssignment.AssignmentStatus.FAILED));
        if (!validTransition)
            throw new BusinessException("Invalid delivery status transition: " + current + " → " + newStatus);

        assignment.setStatus(newStatus);
        Order order = assignment.getOrder();

        if (newStatus == DeliveryAssignment.AssignmentStatus.PICKED_UP) {
            assignment.setPickedUpAt(LocalDateTime.now());
            order.setStatus(Order.OrderStatus.PICKED_UP);
            orderRepo.save(order);
            saveOrderHistory(order, Order.OrderStatus.PICKED_UP.name(), "Picked up by delivery partner");
        }
        if (newStatus == DeliveryAssignment.AssignmentStatus.OUT_FOR_DELIVERY) {
            order.setStatus(Order.OrderStatus.OUT_FOR_DELIVERY);
            orderRepo.save(order);
            saveOrderHistory(order, Order.OrderStatus.OUT_FOR_DELIVERY.name(), "Out for delivery");
        }

        if (newStatus == DeliveryAssignment.AssignmentStatus.DELIVERED) {
            assignment.setDeliveredAt(LocalDateTime.now());
            // Update order
            order.setStatus(Order.OrderStatus.DELIVERED);
            if (order.getPaymentMethod() == Order.PaymentMethod.COD)
                order.setPaymentStatus(Order.PaymentStatus.PAID);
            orderRepo.save(order);
            saveOrderHistory(order, Order.OrderStatus.DELIVERED.name(), "Delivered to customer");
            // Update partner stats
            partner.setTotalDeliveries(partner.getTotalDeliveries() + 1);
            partner.setTotalEarnings(partner.getTotalEarnings().add(assignment.getDeliveryFee()));
            partner.setAvailable(true);
            partnerRepo.save(partner);

            notificationService.send(order.getUser().getId(), "Order Delivered! 🎉",
                    "Your order #" + order.getOrderNumber() + " has been delivered.", "DELIVERY");
        }

        if (newStatus == DeliveryAssignment.AssignmentStatus.FAILED) {
            // Return the order to the pickup queue so an administrator can reassign it.
            order.setStatus(Order.OrderStatus.READY_FOR_PICKUP);
            orderRepo.save(order);
            saveOrderHistory(order, "DELIVERY_FAILED", "Delivery attempt failed; order returned to pickup queue");
            partner.setAvailable(true);
            partnerRepo.save(partner);
        }
        return toResponse(assignmentRepo.save(assignment));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean getAvailability(Long userId) {
        return findPartnerByUserId(userId).isAvailable();
    }

    @Override
    @Transactional
    public void toggleAvailability(Long userId) {
        DeliveryPartner partner = findPartnerByUserId(userId);
        if (!partner.isAvailable()) {
            boolean hasActiveAssignment = assignmentRepo.findByDeliveryPartnerId(partner.getId()).stream()
                    .anyMatch(a -> a.getStatus() != DeliveryAssignment.AssignmentStatus.DELIVERED
                            && a.getStatus() != DeliveryAssignment.AssignmentStatus.FAILED);
            if (hasActiveAssignment)
                throw new BusinessException("Complete your assigned delivery before becoming available again");
        }
        partner.setAvailable(!partner.isAvailable());
        partnerRepo.save(partner);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listPartners() {
        return partnerRepo.findAll().stream().map(p -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.getId());
            m.put("name", p.getUser().getName());
            m.put("phone", p.getUser().getPhone());
            m.put("vehicleType", p.getVehicleType());
            m.put("available", p.isAvailable());
            m.put("totalDeliveries", p.getTotalDeliveries());
            return m;
        }).collect(Collectors.toList());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void saveOrderHistory(Order order, String status, String note) {
        historyRepo.save(OrderStatusHistory.builder().order(order).status(status).note(note).build());
    }

    private DeliveryPartner findPartnerByUserId(Long userId) {
        return partnerRepo.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Delivery partner profile not found for this user"));
    }

    private DeliveryAssignmentResponse toResponse(DeliveryAssignment a) {
        Order order = a.getOrder();
        Address addr = order.getAddress();
        return DeliveryAssignmentResponse.builder()
                .assignmentId(a.getId()).orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .customerName(order.getUser().getName())
                .deliveryAddress(addr.getAddressLine() + ", " + addr.getCity())
                .restaurantName(order.getRestaurant().getName())
                .restaurantAddress(order.getRestaurant().getAddress())
                .totalAmount(order.getTotalAmount())
                .status(a.getStatus().name()).deliveryFee(a.getDeliveryFee())
                .assignedAt(a.getAssignedAt()).pickedUpAt(a.getPickedUpAt())
                .deliveredAt(a.getDeliveredAt()).build();
    }
}
