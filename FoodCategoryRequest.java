package com.fooddelivery.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter
public class FoodCategoryRequest {
    @NotBlank @Size(max=100) private String name;
    @Size(max=300) private String description;
    private Integer displayOrder;
}
