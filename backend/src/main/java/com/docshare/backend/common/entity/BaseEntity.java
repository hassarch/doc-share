package com.docshare.backend.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Shared base for every JPA entity in this system.
 *
 * <p>Provides a UUID primary key plus {@code createdAt}/{@code updatedAt} auditing fields,
 * populated automatically by Spring Data JPA auditing (see {@code JpaAuditingConfig}). Every domain
 * entity (User, Document, Folder, Permission, ...) extends this instead of redeclaring these fields
 * — consistent types (Instant, not a mix of Instant/LocalDateTime/Date) and one place to change the
 * ID generation strategy later if needed.
 *
 * <p>This is a {@code @MappedSuperclass}, not an entity itself — it has no table of its own; its
 * fields are inlined into each subclass's table.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

  @Id
  @GeneratedValue
  @Column(updatable = false, nullable = false)
  private UUID id;

  @CreatedDate
  @Column(updatable = false, nullable = false)
  private Instant createdAt;

  @LastModifiedDate
  @Column(nullable = false)
  private Instant updatedAt;

  public UUID getId() {
    return id;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
