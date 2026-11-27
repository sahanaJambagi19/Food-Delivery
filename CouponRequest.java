package com.fooddelivery.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter
public class CouponRequest {
    @NotBlank @Size(max=50) private String code;
    @Size(max=300) private String description;
    @NotNull private String discountType; // PERCENTAGE | FLAT
    @NotNull @DecimalMin("0.01") private BigDecimal discountValue;
    @DecimalMin("0.0") private BigDecimal minOrderAmount;
    @DecimalMin("0.0") private BigDecimal maxDiscount;
    @Min(1) private Integer usageLimit;
    @NotNull private LocalDateTime validFrom;
    @NotNull private LocalDateTime validUntil;
}
