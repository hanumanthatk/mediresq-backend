package com.smartemergency.service;

import com.smartemergency.dto.response.NotificationResponse;
import com.smartemergency.entity.Notification;
import com.smartemergency.entity.User;
import com.smartemergency.enums.NotificationType;
import com.smartemergency.exception.ResourceNotFoundException;
import com.smartemergency.repository.NotificationRepository;
import com.smartemergency.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Notification Service - manages in-app and real-time notifications.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Async
    public void sendNotification(Long userId, String title, String message, NotificationType type, Long requestId) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) return;

            Notification notification = Notification.builder()
                    .user(user).title(title).message(message)
                    .type(type).relatedRequestId(requestId).isRead(false)
                    .build();
            Notification saved = notificationRepository.save(notification);

            // Push via WebSocket
            messagingTemplate.convertAndSendToUser(
                    user.getEmail(), "/queue/notifications", toResponse(saved));
        } catch (Exception e) {
            log.warn("Notification send failed for userId {}: {}", userId, e.getMessage());
        }
    }

    public List<NotificationResponse> getNotifications(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return notificationRepository.findByUserOrderByCreatedAtDesc(user)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public long getUnreadCount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return notificationRepository.countByUserAndIsRead(user, false);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        notificationRepository.markAllAsReadByUser(user);
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId()).title(n.getTitle()).message(n.getMessage())
                .type(n.getType()).isRead(n.getIsRead())
                .relatedRequestId(n.getRelatedRequestId())
                .createdAt(n.getCreatedAt()).build();
    }
}
