package com.fooddelivery.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @Builder
public class PaymentResponse {
    private Long paymentId;
    private Long orderId;
    private String orderNumber;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private LocalDateTime paidAt;
    private boolean demo;
    private String razorpayKeyId;
}
