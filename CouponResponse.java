package com.fooddelivery.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @Builder
public class CouponResponse {
    private Long id;
    private String code;
    private String description;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal minOrderAmount;
    private BigDecimal maxDiscount;
    private Integer usageLimit;
    private Integer usedCount;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private boolean active;
}
