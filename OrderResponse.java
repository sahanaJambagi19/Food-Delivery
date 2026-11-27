package com.fooddelivery.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @Builder
public class OrderResponse {
    private Long id;
    private String orderNumber;
    private String status;
    private String paymentMethod;
    private String paymentStatus;

    private Long restaurantId;
    private String restaurantName;

    private Long addressId;
    private String deliveryAddress;

    private BigDecimal subtotal;
    private BigDecimal deliveryCharge;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;

    private String couponCode;
    private String specialInstructions;
    private Integer estimatedDeliveryTime;

    private List<OrderItemResponse> items;
    private List<StatusHistoryResponse> statusHistory;
    private LocalDateTime createdAt;

    @Getter @Setter @Builder
    public static class OrderItemResponse {
        private Long foodItemId;
        private String name;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
    }

    @Getter @Setter @Builder
    public static class StatusHistoryResponse {
        private String status;
        private String note;
        private LocalDateTime changedAt;
    }
}
