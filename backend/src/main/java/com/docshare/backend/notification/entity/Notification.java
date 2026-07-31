package com.docshare.backend.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Deliberately does NOT extend {@code common.entity.BaseEntity} — the {@code notifications} table
 * (per the V1 migration) has no {@code updated_at} column; a notification is written once and only
 * ever has its {@code isRead} flag flipped; there's no "last modified" concept worth tracking for
 * it, so forcing the shared base's auditing columns onto this table would mean adding a column with
 * no real use.
 */
@Entity
@Table(name = "notifications")
public class Notification {

  @Id @GeneratedValue private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private NotificationType type;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "payload_json", nullable = false)
  private String payloadJson;

  @Column(name = "is_read", nullable = false)
  private boolean read;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected Notification() {}

  public Notification(UUID userId, NotificationType type, String payloadJson) {
    this.userId = userId;
    this.type = type;
    this.payloadJson = payloadJson;
    this.read = false;
    this.createdAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public UUID getUserId() {
    return userId;
  }

  public NotificationType getType() {
    return type;
  }

  public String getPayloadJson() {
    return payloadJson;
  }

  public boolean isRead() {
    return read;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void markRead() {
    this.read = true;
  }
}
