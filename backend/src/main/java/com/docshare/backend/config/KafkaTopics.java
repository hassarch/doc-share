package com.docshare.backend.config;

/**
 * Central registry of Kafka topic names, so a topic name is never a string literal duplicated
 * between a producer and its consumers — one typo in one of those places would silently create a
 * second, empty topic instead of failing loudly.
 */
public final class KafkaTopics {

  private KafkaTopics() {}

  /** Carries every {@code com.docshare.backend.common.event.DocumentEvent}. */
  public static final String DOCUMENT_EVENTS = "document-events";
}
