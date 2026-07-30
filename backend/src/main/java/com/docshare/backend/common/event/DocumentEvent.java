package com.docshare.backend.common.event;

import java.time.Instant;
import java.util.Map;

/**
 * The event envelope published to the {@code document-events} Kafka topic (FR-11.1), matching the
 * PRD's Appendix schema. {@code eventVersion} exists so this shape can evolve without breaking
 * consumers already deployed against an older version (FR-11.3) — bump it, don't mutate the meaning
 * of existing fields.
 *
 * <p>Lives in {@code common.event} rather than {@code documents} because both the producer ({@code
 * documents.service.DocumentEventPublisher}) and the consumers ({@code notification}, {@code
 * audit}) need this shape, and none of those modules should import from another's internals just to
 * agree on a wire format — a shared schema is exactly what {@code common} is for.
 */
public record DocumentEvent(
    String eventType,
    int eventVersion,
    String documentId,
    String ownerId,
    Instant timestamp,
    Map<String, Object> metadata) {

  public static final String DOCUMENT_UPLOADED = "document.uploaded";
  public static final String DOCUMENT_DELETED = "document.deleted";

  public static final int CURRENT_VERSION = 1;
}
