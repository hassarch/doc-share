package com.docshare.backend.storage.service;

import java.io.InputStream;

/**
 * The only door into physical file storage (FR-5.3): callers pass an opaque object key and get
 * bytes in/out — they never see bucket names, node selection, or any MinIO-specific type. When
 * multi-node storage and chunking (PRD Phase 2/4) arrive, this interface's shape doesn't need to
 * change; only {@link MinioStorageService}'s internals do.
 */
public interface StorageService {

  /** Uploads content under a caller-supplied object key. */
  void upload(String objectKey, InputStream content, long size, String contentType);

  /** Streams content back for a given object key. Caller must close it. */
  InputStream download(String objectKey);

  void delete(String objectKey);
}
