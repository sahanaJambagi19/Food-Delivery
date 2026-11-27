package com.fooddelivery.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @Builder
public class ReviewResponse {
    private Long id;
    private Long restaurantId;
    private String restaurantName;
    private Long userId;
    private String userName;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
