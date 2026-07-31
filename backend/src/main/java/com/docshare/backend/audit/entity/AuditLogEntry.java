package com.docshare.backend.audit.entity;

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
 * An immutable audit record (FR-13.1-13.4). Like {@code notification.entity.Notification}, this
 * does NOT extend {@code common.entity.BaseEntity} — the {@code audit_log} table is genuinely
 * append-only (no {@code updated_at} column in the V1 migration, and no setter on this class beyond
 * the constructor), which is stronger than BaseEntity's auditing already gives you: BaseEntity
 * tracks when a row last changed, but this row is designed to never change at all once written.
 */
@Entity
@Table(name = "audit_log")
public class AuditLogEntry {

  @Id @GeneratedValue private UUID id;

  @Column(name = "actor_id", nullable = false)
  private UUID actorId;

  @Column(nullable = false)
  private String action;

  @Column(name = "target_type", nullable = false)
  private String targetType;

  @Column(name = "target_id", nullable = false)
  private UUID targetId;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AuditResult result;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "metadata_json")
  private String metadataJson;

  protected AuditLogEntry() {}

  public AuditLogEntry(
      UUID actorId,
      String action,
      String targetType,
      UUID targetId,
      AuditResult result,
      String metadataJson) {
    this.actorId = actorId;
    this.action = action;
    this.targetType = targetType;
    this.targetId = targetId;
    this.occurredAt = Instant.now();
    this.result = result;
    this.metadataJson = metadataJson;
  }

  public UUID getId() {
    return id;
  }

  public UUID getActorId() {
    return actorId;
  }

  public String getAction() {
    return action;
  }

  public String getTargetType() {
    return targetType;
  }

  public UUID getTargetId() {
    return targetId;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }

  public AuditResult getResult() {
    return result;
  }

  public String getMetadataJson() {
    return metadataJson;
  }
}
