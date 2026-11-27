package com.fooddelivery.dto.response;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @Builder
public class RestaurantResponse {
    private Long id;
    private String name;
    private String description;
    private String cuisineType;
    private String address;
    private String city;
    private String pincode;
    private String phone;
    private String email;
    private String logoUrl;
    private String coverImageUrl;
    private String openingTime;
    private String closingTime;
    private Integer avgDeliveryTime;
    private BigDecimal minOrderAmount;
    private BigDecimal deliveryCharge;
    private BigDecimal avgRating;
    private Integer totalRatings;
    private boolean open;
    private boolean approved;
    private boolean active;
    private Long ownerId;
    private String ownerName;
}
