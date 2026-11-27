package com.fooddelivery.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @Builder
public class NotificationResponse {
    private Long id;
    private String title;
    private String message;
    private String type;
    private boolean read;
    private LocalDateTime createdAt;
}
