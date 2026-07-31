package com.docshare.backend.audit.consumer;

import com.docshare.backend.audit.service.AuditService;
import com.docshare.backend.common.event.DocumentEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Subscribes to {@code document-events} with its own consumer group ({@code audit-service}) —
 * separate from {@code notification-service}'s — so both consumers receive every event
 * independently. Unlike the Notification consumer, this one logs <em>every</em> event type: an
 * audit trail cares about all actions, not just the ones worth surfacing to a person.
 */
@Component
public class AuditEventConsumer {

  private static final Logger log = LoggerFactory.getLogger(AuditEventConsumer.class);

  private final AuditService auditService;
  private final ObjectMapper objectMapper;

  public AuditEventConsumer(AuditService auditService, ObjectMapper objectMapper) {
    this.auditService = auditService;
    this.objectMapper = objectMapper;
  }

  @KafkaListener(
      topics = com.docshare.backend.config.KafkaTopics.DOCUMENT_EVENTS,
      groupId = "audit-service")
  public void onDocumentEvent(DocumentEvent event) {
    try {
      String metadataJson = objectMapper.writeValueAsString(event.metadata());
      auditService.record(
          UUID.fromString(event.ownerId()),
          event.eventType(),
          "document",
          UUID.fromString(event.documentId()),
          metadataJson);
    } catch (Exception e) {
      log.error("Failed to record audit entry for event {}", event.eventType(), e);
    }
  }
}
