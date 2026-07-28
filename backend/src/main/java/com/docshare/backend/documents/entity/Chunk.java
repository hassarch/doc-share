package com.docshare.backend.documents.entity;

import com.docshare.backend.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * One chunk of a large, split file (FR-7.1-7.4). {@code storageNodeId} is a plain UUID, not a JPA
 * relationship to {@code storage.entity.StorageNode} — same cross-module reasoning as {@link
 * Document#getOwnerId()}: the {@code documents} module doesn't reach into {@code storage}'s
 * entities directly.
 */
@Entity
@Table(name = "chunks")
public class Chunk extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "document_version_id", nullable = false)
  private DocumentVersion documentVersion;

  @Column(name = "chunk_number", nullable = false)
  private int chunkNumber;

  @Column(name = "storage_node_id", nullable = false)
  private UUID storageNodeId;

  @Column(nullable = false)
  private String checksum;

  @Column(name = "size_bytes", nullable = false)
  private long sizeBytes;

  protected Chunk() {}

  public Chunk(
      DocumentVersion documentVersion,
      int chunkNumber,
      UUID storageNodeId,
      String checksum,
      long sizeBytes) {
    this.documentVersion = documentVersion;
    this.chunkNumber = chunkNumber;
    this.storageNodeId = storageNodeId;
    this.checksum = checksum;
    this.sizeBytes = sizeBytes;
  }

  public DocumentVersion getDocumentVersion() {
    return documentVersion;
  }

  public int getChunkNumber() {
    return chunkNumber;
  }

  public UUID getStorageNodeId() {
    return storageNodeId;
  }

  public String getChecksum() {
    return checksum;
  }

  public long getSizeBytes() {
    return sizeBytes;
  }
}
