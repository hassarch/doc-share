package com.docshare.backend.notification.service;

import com.docshare.backend.common.exception.NotFoundException;
import com.docshare.backend.notification.dto.NotificationResponse;
import com.docshare.backend.notification.entity.Notification;
import com.docshare.backend.notification.entity.NotificationType;
import com.docshare.backend.notification.repository.NotificationRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

  private final NotificationRepository notificationRepository;
  private final SimpMessagingTemplate messagingTemplate;

  public NotificationService(
      NotificationRepository notificationRepository, SimpMessagingTemplate messagingTemplate) {
    this.notificationRepository = notificationRepository;
    this.messagingTemplate = messagingTemplate;
  }

  /**
   * Persists a notification and pushes it live over WebSocket (FR-12.6). If the user isn't
   * currently connected, the push is a no-op — they'll still see it via {@link #listForUser(UUID)}
   * on their next visit, since it's already durably stored before the push is attempted.
   */
  @Transactional
  public void notify(UUID userId, NotificationType type, String payloadJson) {
    Notification notification =
        notificationRepository.save(new Notification(userId, type, payloadJson));

    messagingTemplate.convertAndSendToUser(
        userId.toString(), "/queue/notifications", toResponse(notification));
  }

  public List<NotificationResponse> listForUser(UUID userId) {
    return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
        .map(NotificationService::toResponse)
        .toList();
  }

  public long unreadCount(UUID userId) {
    return notificationRepository.countByUserIdAndReadFalse(userId);
  }

  @Transactional
  public void markRead(UUID notificationId, UUID requesterId) {
    Notification notification =
        notificationRepository
            .findById(notificationId)
            .filter(n -> n.getUserId().equals(requesterId))
            .orElseThrow(() -> new NotFoundException("Notification not found"));
    notification.markRead();
    notificationRepository.save(notification);
  }

  private static NotificationResponse toResponse(Notification notification) {
    return new NotificationResponse(
        notification.getId(),
        notification.getType().name(),
        notification.getPayloadJson(),
        notification.isRead(),
        notification.getCreatedAt());
  }
}
