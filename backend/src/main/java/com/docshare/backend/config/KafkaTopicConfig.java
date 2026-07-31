package com.docshare.backend.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declares topics as Spring beans so they're created automatically on application startup against
 * the local Kafka broker (KAFKA_AUTO_CREATE_TOPICS_ENABLE is also on in the Docker Compose broker,
 * but declaring topics explicitly means partition count and replication factor are controlled here,
 * not left to broker defaults).
 */
@Configuration
public class KafkaTopicConfig {

  @Bean
  public NewTopic documentEventsTopic() {
    return TopicBuilder.name(KafkaTopics.DOCUMENT_EVENTS)
        .partitions(3) // partitioned by documentId key, below, for per-document ordering
        .replicas(1) // single-broker local dev; real replication factor is a Deployment-phase
        // concern
        .build();
  }
}
