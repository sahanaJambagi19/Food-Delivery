package com.fooddelivery.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Getter @Setter
public class RestaurantRequest {
    @NotBlank @Size(max=200) private String name;
    private String description;
    @Size(max=100) private String cuisineType;
    @NotBlank @Size(max=300) private String address;
    @NotBlank @Size(max=100) private String city;
    @Size(max=10) private String pincode;
    @Size(max=20) private String phone;
    @Email @Size(max=150) private String email;
    private String logoUrl;
    private String coverImageUrl;
    @Pattern(regexp="^([01]\\d|2[0-3]):[0-5]\\d$", message="Opening time must use HH:mm format")
    private String openingTime;
    @Pattern(regexp="^([01]\\d|2[0-3]):[0-5]\\d$", message="Closing time must use HH:mm format")
    private String closingTime;
    @Min(1) @Max(240) private Integer avgDeliveryTime;
    @DecimalMin("0.0") private BigDecimal minOrderAmount;
    @DecimalMin("0.0") private BigDecimal deliveryCharge;
}
