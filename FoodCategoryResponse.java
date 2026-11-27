package com.fooddelivery.dto.response;

import lombok.*;

@Getter @Setter @Builder
public class FoodCategoryResponse {
    private Long id;
    private String name;
    private String description;
    private Integer displayOrder;
    private boolean active;
    private Long restaurantId;
}
