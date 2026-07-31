package com.docshare.backend.documents.event;

import com.docshare.backend.common.event.DocumentEvent;
import com.docshare.backend.config.KafkaTopics;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes {@link DocumentEvent}s. {@link com.docshare.backend.documents.service.DocumentService}
 * calls this after a write succeeds — never before, and never in a way that makes the write's
 * success depend on Kafka being reachable (FR-21.4: uploads succeed on the primary write path; side
 * effects proceed asynchronously).
 *
 * <p>Events are keyed by {@code documentId} so Kafka guarantees ordering for events about the
 * <em>same</em> document (they land in the same partition) without needing a single-partition
 * topic, which would remove the throughput benefit of partitioning at all.
 */
@Component
public class DocumentEventPublisher {

  private static final Logger log = LoggerFactory.getLogger(DocumentEventPublisher.class);

  private final KafkaTemplate<String, DocumentEvent> kafkaTemplate;

  public DocumentEventPublisher(KafkaTemplate<String, DocumentEvent> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }

  public void publishUploaded(UUID documentId, UUID ownerId, String filename, long sizeBytes) {
    publish(
        DocumentEvent.DOCUMENT_UPLOADED,
        documentId,
        ownerId,
        Map.of("filename", filename, "sizeBytes", sizeBytes));
  }

  public void publishDeleted(UUID documentId, UUID ownerId, String filename) {
    publish(DocumentEvent.DOCUMENT_DELETED, documentId, ownerId, Map.of("filename", filename));
  }

  private void publish(
      String eventType, UUID documentId, UUID ownerId, Map<String, Object> metadata) {
    DocumentEvent event =
        new DocumentEvent(
            eventType,
            DocumentEvent.CURRENT_VERSION,
            documentId.toString(),
            ownerId.toString(),
            Instant.now(),
            metadata);

    // Per FR-21.5: a failed publish is logged for manual reconciliation,
    // not thrown - the caller's write already succeeded and must not be
    // rolled back just because the event bus had a bad moment.
    kafkaTemplate
        .send(KafkaTopics.DOCUMENT_EVENTS, documentId.toString(), event)
        .whenComplete(
            (result, ex) -> {
              if (ex != null) {
                log.error("Failed to publish {} for document {}", eventType, documentId, ex);
              }
            });
  }
}
