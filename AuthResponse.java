package com.fooddelivery.dto.response;

import lombok.*;

@Getter @Setter @Builder
public class AuthResponse {
    private Long userId;
    private String name;
    private String email;
    private String role;
    private String accessToken;
    private String refreshToken;
}
