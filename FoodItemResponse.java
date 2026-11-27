package com.fooddelivery.dto.response;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @Builder
public class FoodItemResponse {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private String imageUrl;
    private boolean veg;
    private boolean available;
    private BigDecimal avgRating;
    private Long categoryId;
    private String categoryName;
    private Long restaurantId;
    private String restaurantName;
}
