package com.docshare.backend.documents.entity;

import com.docshare.backend.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * An immutable snapshot of a document's content at a point in time (FR-8.1 — every edit creates a
 * new version, never a mutation of the existing one). {@code storageRef} is a JSON blob describing
 * either a single object reference or an ordered chunk list (FR-7.5) — kept schemaless at the DB
 * level since its shape genuinely differs between the two cases.
 */
@Entity
@Table(name = "document_versions")
public class DocumentVersion extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "document_id", nullable = false)
  private Document document;

  @Column(name = "version_number", nullable = false)
  private int versionNumber;

  @Column(name = "size_bytes", nullable = false)
  private long sizeBytes;

  @Column(name = "sha256_hash", nullable = false)
  private String sha256Hash;

  @Column(name = "created_by", nullable = false)
  private UUID createdBy;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "storage_ref", nullable = false)
  private String storageRef;

  protected DocumentVersion() {}

  public DocumentVersion(
      Document document,
      int versionNumber,
      long sizeBytes,
      String sha256Hash,
      UUID createdBy,
      String storageRefJson) {
    this.document = document;
    this.versionNumber = versionNumber;
    this.sizeBytes = sizeBytes;
    this.sha256Hash = sha256Hash;
    this.createdBy = createdBy;
    this.storageRef = storageRefJson;
  }

  public Document getDocument() {
    return document;
  }

  public int getVersionNumber() {
    return versionNumber;
  }

  public long getSizeBytes() {
    return sizeBytes;
  }

  public String getSha256Hash() {
    return sha256Hash;
  }

  public UUID getCreatedBy() {
    return createdBy;
  }

  public String getStorageRef() {
    return storageRef;
  }
}
