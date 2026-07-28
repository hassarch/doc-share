package com.docshare.backend.sharing.entity;

import com.docshare.backend.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

/**
 * One user's role on one document (FR-4.1/4.2). {@code documentId} and {@code userId} are plain
 * UUIDs, not JPA relationships — {@code sharing} doesn't reach into {@code documents} or {@code
 * users} entities directly, same cross-module pattern as elsewhere in this codebase.
 */
@Entity
@Table(
    name = "permissions",
    uniqueConstraints = @UniqueConstraint(columnNames = {"document_id", "user_id"}))
public class Permission extends BaseEntity {

  @Column(name = "document_id", nullable = false)
  private UUID documentId;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PermissionRole role;

  @Column(name = "granted_by", nullable = false)
  private UUID grantedBy;

  @Column(name = "granted_at", nullable = false)
  private Instant grantedAt;

  protected Permission() {}

  public Permission(UUID documentId, UUID userId, PermissionRole role, UUID grantedBy) {
    this.documentId = documentId;
    this.userId = userId;
    this.role = role;
    this.grantedBy = grantedBy;
    this.grantedAt = Instant.now();
  }

  public UUID getDocumentId() {
    return documentId;
  }

  public UUID getUserId() {
    return userId;
  }

  public PermissionRole getRole() {
    return role;
  }

  public UUID getGrantedBy() {
    return grantedBy;
  }

  public Instant getGrantedAt() {
    return grantedAt;
  }

  public void changeRole(PermissionRole newRole) {
    this.role = newRole;
  }
}
