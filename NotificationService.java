package com.fooddelivery.service;

import com.fooddelivery.dto.response.NotificationResponse;
import java.util.List;

public interface NotificationService {
    void send(Long userId, String title, String message, String type);
    List<NotificationResponse> getForUser(Long userId);
    long countUnread(Long userId);
    void markAllRead(Long userId);
    void markRead(Long userId, Long notificationId);
}
