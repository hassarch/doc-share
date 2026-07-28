package com.docshare.backend.documents.entity;

import com.docshare.backend.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

/** A folder, optionally nested under another folder (FR-3.6). */
@Entity
@Table(name = "folders")
public class Folder extends BaseEntity {

  @Column(name = "owner_id", nullable = false)
  private UUID ownerId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_folder_id")
  private Folder parentFolder;

  @Column(nullable = false)
  private String name;

  protected Folder() {}

  public Folder(UUID ownerId, Folder parentFolder, String name) {
    this.ownerId = ownerId;
    this.parentFolder = parentFolder;
    this.name = name;
  }

  public UUID getOwnerId() {
    return ownerId;
  }

  public Folder getParentFolder() {
    return parentFolder;
  }

  public String getName() {
    return name;
  }

  public void rename(String newName) {
    this.name = newName;
  }
}
