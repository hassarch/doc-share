package com.docshare.backend.sharing.entity;

import com.docshare.backend.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A public share link for a document (FR-4.4-4.7): optional expiration, optional password, optional
 * download limit, optionally read-only. {@code documentId} is a plain UUID for the same
 * cross-module reasons as {@link Permission}.
 */
@Entity
@Table(name = "share_links")
public class ShareLink extends BaseEntity {

  @Column(name = "document_id", nullable = false)
  private UUID documentId;

  @Column(nullable = false, unique = true)
  private String token;

  @Column(name = "expires_at")
  private Instant expiresAt;

  @Column(name = "password_hash")
  private String passwordHash;

  @Column(name = "download_limit")
  private Integer downloadLimit;

  @Column(name = "downloads_used", nullable = false)
  private int downloadsUsed;

  @Column(name = "read_only", nullable = false)
  private boolean readOnly;

  protected ShareLink() {}

  public ShareLink(
      UUID documentId,
      String token,
      Instant expiresAt,
      String passwordHash,
      Integer downloadLimit,
      boolean readOnly) {
    this.documentId = documentId;
    this.token = token;
    this.expiresAt = expiresAt;
    this.passwordHash = passwordHash;
    this.downloadLimit = downloadLimit;
    this.downloadsUsed = 0;
    this.readOnly = readOnly;
  }

  public UUID getDocumentId() {
    return documentId;
  }

  public String getToken() {
    return token;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public Integer getDownloadLimit() {
    return downloadLimit;
  }

  public int getDownloadsUsed() {
    return downloadsUsed;
  }

  public boolean isReadOnly() {
    return readOnly;
  }

  public boolean isExpired() {
    return expiresAt != null && Instant.now().isAfter(expiresAt);
  }

  public boolean isDownloadLimitReached() {
    return downloadLimit != null && downloadsUsed >= downloadLimit;
  }

  public void recordDownload() {
    this.downloadsUsed++;
  }
}
