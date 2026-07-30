package com.docshare.backend.documents.service;

/**
 * The shape stored in {@code DocumentVersion.storageRef} as JSON. {@code
 * type} is {@code "single"} for this phase (one object, one node) — a
 * {@code "chunked"} variant with a chunk list is added in the Chunking
 * phase (FR-7.1-7.5), at which point this record gains a sibling, not a
 * rewrite.
 */
public record StorageRef(String type, String objectKey) {

  public static StorageRef single(String objectKey) {
    return new StorageRef("single", objectKey);
  }
}
