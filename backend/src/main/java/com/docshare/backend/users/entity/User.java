package com.docshare.backend.users.entity;

import com.docshare.backend.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * A registered user. Owned by the {@code users} module even though {@code password_hash} is
 * arguably an auth concern — splitting "the user record" across two tables would complicate every
 * join without real benefit at this scale. The Auth module consumes this entity through a {@code
 * users.service} interface (per ADR-0001 #2's boundary rule), never through {@code
 * users.repository} directly.
 */
@Entity
@Table(name = "users")
public class User extends BaseEntity {

  @Column(nullable = false, unique = true)
  private String email;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Column(nullable = false)
  private String name;

  @Column(name = "storage_quota_bytes", nullable = false)
  private long storageQuotaBytes;

  @Column(name = "storage_used_bytes", nullable = false)
  private long storageUsedBytes;

  protected User() {
    // JPA requires a no-arg constructor; not for direct use.
  }

  public User(String email, String passwordHash, String name, long storageQuotaBytes) {
    this.email = email;
    this.passwordHash = passwordHash;
    this.name = name;
    this.storageQuotaBytes = storageQuotaBytes;
    this.storageUsedBytes = 0L;
  }

  public String getEmail() {
    return email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public String getName() {
    return name;
  }

  public long getStorageQuotaBytes() {
    return storageQuotaBytes;
  }

  public long getStorageUsedBytes() {
    return storageUsedBytes;
  }

  public void recordStorageUsed(long deltaBytes) {
    long updated = this.storageUsedBytes + deltaBytes;
    if (updated < 0) {
      throw new IllegalStateException("storageUsedBytes cannot go negative");
    }
    this.storageUsedBytes = updated;
  }
}
