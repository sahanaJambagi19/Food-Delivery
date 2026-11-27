package com.fooddelivery.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Getter @Setter
public class FoodItemRequest {
    @NotNull private Long categoryId;
    @NotBlank @Size(max=200) private String name;
    private String description;
    @NotNull @DecimalMin("0.01") private BigDecimal price;
    private String imageUrl;
    private boolean veg = true;
    private boolean available = true;
}
