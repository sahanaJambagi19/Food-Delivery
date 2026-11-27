package com.fooddelivery.dto.response;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @Builder
public class CouponValidateResponse {
    private Long couponId;
    private String code;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal discountAmount;   // actual amount saved
    private BigDecimal finalAmount;      // after discount
}
