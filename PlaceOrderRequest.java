package com.fooddelivery.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter
public class PlaceOrderRequest {
    @NotNull private Long addressId;
    @NotBlank private String paymentMethod;  // ONLINE | COD
    private String couponCode;
    private String specialInstructions;
}
