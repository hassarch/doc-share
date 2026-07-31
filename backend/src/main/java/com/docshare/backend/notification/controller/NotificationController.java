package com.docshare.backend.notification.controller;

import com.docshare.backend.common.util.CurrentUser;
import com.docshare.backend.notification.dto.NotificationResponse;
import com.docshare.backend.notification.service.NotificationService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

  private final NotificationService notificationService;

  public NotificationController(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @GetMapping
  public ResponseEntity<List<NotificationResponse>> list() {
    return ResponseEntity.ok(notificationService.listForUser(CurrentUser.id()));
  }

  @GetMapping("/unread-count")
  public ResponseEntity<Map<String, Long>> unreadCount() {
    return ResponseEntity.ok(Map.of("count", notificationService.unreadCount(CurrentUser.id())));
  }

  @PatchMapping("/{id}/read")
  public ResponseEntity<Void> markRead(@PathVariable UUID id) {
    notificationService.markRead(id, CurrentUser.id());
    return ResponseEntity.noContent().build();
  }
}
