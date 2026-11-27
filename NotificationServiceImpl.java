package com.fooddelivery.service.impl;

import com.fooddelivery.dto.response.NotificationResponse;
import com.fooddelivery.entity.*;
import com.fooddelivery.exception.*;
import com.fooddelivery.repository.*;
import com.fooddelivery.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepo;
    private final UserRepository userRepo;

    @Override
    @Transactional
    public void send(Long userId, String title, String message, String type) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        notificationRepo.save(Notification.builder()
                .user(user).title(title).message(message).type(type).read(false).build());
    }

    @Override
    public List<NotificationResponse> getForUser(Long userId) {
        return notificationRepo.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public long countUnread(Long userId) {
        return notificationRepo.countByUserIdAndReadFalse(userId);
    }

    @Override
    @Transactional
    public void markAllRead(Long userId) {
        notificationRepo.findByUserIdOrderByCreatedAtDesc(userId).forEach(n -> {
            n.setRead(true);
            notificationRepo.save(n);
        });
    }

    @Override
    @Transactional
    public void markRead(Long userId, Long notificationId) {
        Notification n = notificationRepo.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));
        if (!n.getUser().getId().equals(userId))
            throw new BusinessException("Notification does not belong to this user");
        n.setRead(true);
        notificationRepo.save(n);
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId()).title(n.getTitle()).message(n.getMessage())
                .type(n.getType()).read(n.isRead()).createdAt(n.getCreatedAt()).build();
    }
}
