package com.fooddelivery.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter
public class PaymentVerifyRequest {
    @NotBlank private String razorpayOrderId;
    @NotBlank private String razorpayPaymentId;
    @NotBlank private String razorpaySignature;
}
