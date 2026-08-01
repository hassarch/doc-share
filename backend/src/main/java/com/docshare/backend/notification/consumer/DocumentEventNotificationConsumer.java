package com.docshare.backend.notification.consumer;

import com.docshare.backend.common.event.DocumentEvent;
import com.docshare.backend.notification.entity.NotificationType;
import com.docshare.backend.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Subscribes to {@code document-events} independently of {@code
 * documents.event.DocumentEventPublisher} — per FR-11.2, the producer has no idea this consumer
 * exists. If this consumer is down, events queue in Kafka and process on recovery (FR-21.2);
 * uploads never wait on this.
 *
 * <p>Consumer group {@code notification-service} — a separate group ID from {@link
 * com.docshare.backend.audit.consumer.AuditEventConsumer}'s group means both consumers receive
 * every event independently (each group gets its own copy of the topic), rather than competing for
 * the same messages.
 */
@Component
public class DocumentEventNotificationConsumer {

  private static final Logger log =
      LoggerFactory.getLogger(DocumentEventNotificationConsumer.class);

  private final NotificationService notificationService;
  private final ObjectMapper objectMapper;

  public DocumentEventNotificationConsumer(
      NotificationService notificationService, ObjectMapper objectMapper) {
    this.notificationService = notificationService;
    this.objectMapper = objectMapper;
  }

  @KafkaListener(
      topics = com.docshare.backend.config.KafkaTopics.DOCUMENT_EVENTS,
      groupId = "notification-service")
  public void onDocumentEvent(DocumentEvent event) {
    // Idempotency note (FR-11.4, at-least-once delivery): this handler is
    // NOT idempotent yet — a redelivered event would create a duplicate
    // notification. Acceptable for this phase's demo scope; a real
    // dedup key (e.g. a unique constraint on (userId, eventType,
    // documentId, eventTimestamp)) is a hardening item for the
    // Production Readiness phase, flagged here rather than silently
    // skipped.
    if (DocumentEvent.DOCUMENT_UPLOADED.equals(event.eventType())) {
      handleUploaded(event);
    }
    // document.deleted intentionally produces no notification - deleting
    // your own file isn't news to you. Audit still logs it (see
    // AuditEventConsumer) since audit trails care about every action,
    // notifications only care about ones worth surfacing to a person.
  }

  private void handleUploaded(DocumentEvent event) {
    try {
      String payloadJson = objectMapper.writeValueAsString(event.metadata());
      notificationService.notify(
          UUID.fromString(event.ownerId()), NotificationType.UPLOAD_COMPLETE, payloadJson);
    } catch (Exception e) {
      log.error("Failed to process document.uploaded event for {}", event.documentId(), e);
    }
  }
}
