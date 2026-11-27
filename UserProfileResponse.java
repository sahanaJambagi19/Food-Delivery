package com.fooddelivery.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @Builder
public class UserProfileResponse {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String role;
    private String profileImage;
    private boolean active;
    private LocalDateTime createdAt;
}
