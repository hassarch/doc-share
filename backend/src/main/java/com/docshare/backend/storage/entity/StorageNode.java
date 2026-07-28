package com.docshare.backend.storage.entity;

import com.docshare.backend.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * A registered physical storage node (a MinIO container/bucket in local dev; could be any
 * S3-compatible target in a real deployment). Per FR-5.5, new nodes are added via
 * config/registration, not code changes — this table is that registry.
 */
@Entity
@Table(name = "storage_nodes")
public class StorageNode extends BaseEntity {

  @Column(nullable = false, unique = true)
  private String name;

  @Column(nullable = false)
  private String endpoint;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private StorageNodeStatus status;

  @Column(name = "capacity_bytes", nullable = false)
  private long capacityBytes;

  @Column(name = "used_bytes", nullable = false)
  private long usedBytes;

  protected StorageNode() {}

  public StorageNode(String name, String endpoint, long capacityBytes) {
    this.name = name;
    this.endpoint = endpoint;
    this.status = StorageNodeStatus.ACTIVE;
    this.capacityBytes = capacityBytes;
    this.usedBytes = 0L;
  }

  public String getName() {
    return name;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public StorageNodeStatus getStatus() {
    return status;
  }

  public long getCapacityBytes() {
    return capacityBytes;
  }

  public long getUsedBytes() {
    return usedBytes;
  }

  public void markStatus(StorageNodeStatus newStatus) {
    this.status = newStatus;
  }
}
