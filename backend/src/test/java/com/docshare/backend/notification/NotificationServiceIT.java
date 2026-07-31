package com.docshare.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.docshare.backend.AbstractPostgresIntegrationTest;
import com.docshare.backend.notification.entity.NotificationType;
import com.docshare.backend.notification.repository.NotificationRepository;
import com.docshare.backend.notification.service.NotificationService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class NotificationServiceIT extends AbstractPostgresIntegrationTest {

  @Autowired private NotificationService notificationService;
  @Autowired private NotificationRepository notificationRepository;

  @Test
  void notify_persistsNotification() {
    UUID userId = UUID.randomUUID();
    String payload = "{\"filename\":\"test.pdf\",\"sizeBytes\":1024}";

    notificationService.notify(userId, NotificationType.UPLOAD_COMPLETE, payload);

    var notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    assertThat(notifications).hasSize(1);
    assertThat(notifications.get(0).getType()).isEqualTo(NotificationType.UPLOAD_COMPLETE);
    assertThat(notifications.get(0).getPayloadJson()).isEqualTo(payload);
    assertThat(notifications.get(0).isRead()).isFalse();
  }

  @Test
  void listForUser_returnsNotificationsNewestFirst() {
    UUID userId = UUID.randomUUID();
    notificationService.notify(userId, NotificationType.UPLOAD_COMPLETE, "{}");
    notificationService.notify(userId, NotificationType.UPLOAD_COMPLETE, "{}");

    var responses = notificationService.listForUser(userId);

    assertThat(responses).hasSize(2);
    assertThat(responses.get(0).createdAt()).isAfter(responses.get(1).createdAt());
  }

  @Test
  void unreadCount_countsUnreadOnly() {
    UUID userId = UUID.randomUUID();
    notificationService.notify(userId, NotificationType.UPLOAD_COMPLETE, "{}");
    notificationService.notify(userId, NotificationType.UPLOAD_COMPLETE, "{}");

    var notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    notificationService.markRead(notifications.get(0).getId(), userId);

    assertThat(notificationService.unreadCount(userId)).isEqualTo(1);
  }
}
