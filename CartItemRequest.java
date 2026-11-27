package com.fooddelivery.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter
public class CartItemRequest {
    @NotNull private Long foodItemId;
    @NotNull @Min(1) @Max(20) private Integer quantity;
}
