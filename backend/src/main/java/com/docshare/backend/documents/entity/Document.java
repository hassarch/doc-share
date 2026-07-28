package com.docshare.backend.documents.entity;

import com.docshare.backend.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Document metadata — never file bytes (FR-5.1). The actual bytes live on a storage node,
 * referenced indirectly via {@link DocumentVersion#storageRef}.
 *
 * <p><strong>Cross-module reference pattern:</strong> {@code ownerId} is a plain UUID column, not
 * a JPA {@code @ManyToOne} to {@code users.entity.User}. A JPA relationship would force Hibernate
 * to know how to load a {@code User} directly from the {@code documents} module, defeating the
 * module-boundary rule from ADR-0001 #2 at the ORM level. When code in this module needs user
 * details (e.g. the owner's display name), it asks a {@code users.service} interface for them — it
 * does not navigate a JPA association into another module's entity. {@code currentVersion} is the
 * one exception: it's a same-module relationship (both entities live in {@code documents}), so a
 * real {@code @ManyToOne} is appropriate there.
 */
@Entity
@Table(name = "documents")
public class Document extends BaseEntity {

  @Column(name = "owner_id", nullable = false)
  private UUID ownerId;

  @Column(nullable = false)
  private String filename;

  @Column(name = "folder_id")
  private UUID folderId;

  @Column(name = "size_bytes", nullable = false)
  private long sizeBytes;

  @Column(name = "mime_type", nullable = false)
  private String mimeType;

  @Column(name = "sha256_hash", nullable = false)
  private String sha256Hash;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "current_version_id")
  private DocumentVersion currentVersion;

  @Column(name = "is_deleted", nullable = false)
  private boolean deleted;

  @Enumerated(EnumType.STRING)
  @Column(name = "replication_status", nullable = false)
  private ReplicationStatus replicationStatus;

  protected Document() {}

  public Document(
      UUID ownerId, String filename, UUID folderId, long sizeBytes, String mimeType,
      String sha256Hash) {
    this.ownerId = ownerId;
    this.filename = filename;
    this.folderId = folderId;
    this.sizeBytes = sizeBytes;
    this.mimeType = mimeType;
    this.sha256Hash = sha256Hash;
    this.deleted = false;
    this.replicationStatus = ReplicationStatus.PENDING;
  }

  public UUID getOwnerId() {
    return ownerId;
  }

  public String getFilename() {
    return filename;
  }

  public UUID getFolderId() {
    return folderId;
  }

  public long getSizeBytes() {
    return sizeBytes;
  }

  public String getMimeType() {
    return mimeType;
  }

  public String getSha256Hash() {
    return sha256Hash;
  }

  public DocumentVersion getCurrentVersion() {
    return currentVersion;
  }

  public boolean isDeleted() {
    return deleted;
  }

  public ReplicationStatus getReplicationStatus() {
    return replicationStatus;
  }

  public void rename(String newFilename) {
    this.filename = newFilename;
  }

  public void moveTo(UUID newFolderId) {
    this.folderId = newFolderId;
  }

  public void softDelete() {
    this.deleted = true;
  }

  public void setCurrentVersion(DocumentVersion version) {
    this.currentVersion = version;
  }

  public void markReplicationStatus(ReplicationStatus status) {
    this.replicationStatus = status;
  }
}
