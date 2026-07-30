package com.docshare.backend.storage.service;

import com.docshare.backend.storage.config.StorageProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import org.springframework.stereotype.Service;

/**
 * MinIO-backed implementation of {@link StorageService}. Single-bucket,
 * single-node for this phase — the {@code storage_nodes} table (Phase 4)
 * and multi-node routing land with the Multi-Node Storage/Replication
 * phase, not here.
 */
@Service
public class MinioStorageService implements StorageService {

  private final MinioClient minioClient;
  private final String bucket;

  public MinioStorageService(MinioClient minioClient, StorageProperties properties) {
    this.minioClient = minioClient;
    this.bucket = properties.getBucket();
  }

  /** Ensures the bucket exists once at startup rather than on every upload. */
  @PostConstruct
  void ensureBucketExists() {
    try {
      boolean exists =
          minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
      if (!exists) {
        minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
      }
    } catch (Exception e) {
      throw new StorageException("Failed to initialize storage bucket: " + bucket, e);
    }
  }

  @Override
  public void upload(String objectKey, InputStream content, long size, String contentType) {
    try {
      minioClient.putObject(
          PutObjectArgs.builder().bucket(bucket).object(objectKey).stream(content, size, -1)
              .contentType(contentType)
              .build());
    } catch (Exception e) {
      throw new StorageException("Failed to upload object: " + objectKey, e);
    }
  }

  @Override
  public InputStream download(String objectKey) {
    try {
      return minioClient.getObject(
          GetObjectArgs.builder().bucket(bucket).object(objectKey).build());
    } catch (Exception e) {
      throw new StorageException("Failed to download object: " + objectKey, e);
    }
  }

  @Override
  public void delete(String objectKey) {
    try {
      minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(objectKey).build());
    } catch (Exception e) {
      throw new StorageException("Failed to delete object: " + objectKey, e);
    }
  }

  /**
   * Wraps every MinIO SDK checked exception (it throws about seven
   * different checked types across its API) into one unchecked type, so
   * callers of {@link StorageService} don't need to know MinIO exists.
   */
  public static class StorageException extends RuntimeException {
    public StorageException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
