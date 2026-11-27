package com.fooddelivery.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @Builder
public class DeliveryAssignmentResponse {
    private Long assignmentId;
    private Long orderId;
    private String orderNumber;
    private String customerName;
    private String deliveryAddress;
    private String restaurantName;
    private String restaurantAddress;
    private BigDecimal totalAmount;
    private String status;
    private BigDecimal deliveryFee;
    private LocalDateTime assignedAt;
    private LocalDateTime pickedUpAt;
    private LocalDateTime deliveredAt;
}
