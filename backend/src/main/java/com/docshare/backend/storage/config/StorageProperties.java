package com.docshare.backend.storage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Typed binding for {@code docshare.storage.*}, present since Phase 2. */
@Component
@ConfigurationProperties(prefix = "docshare.storage")
public class StorageProperties {

  private final Minio minio = new Minio();
  private String bucket;
  private long chunkThresholdMb;
  private long chunkSizeMb;

  public Minio getMinio() {
    return minio;
  }

  public String getBucket() {
    return bucket;
  }

  public void setBucket(String bucket) {
    this.bucket = bucket;
  }

  public long getChunkThresholdMb() {
    return chunkThresholdMb;
  }

  public void setChunkThresholdMb(long chunkThresholdMb) {
    this.chunkThresholdMb = chunkThresholdMb;
  }

  public long getChunkSizeMb() {
    return chunkSizeMb;
  }

  public void setChunkSizeMb(long chunkSizeMb) {
    this.chunkSizeMb = chunkSizeMb;
  }

  public static class Minio {
    private String endpoint;
    private String accessKey;
    private String secretKey;

    public String getEndpoint() {
      return endpoint;
    }

    public void setEndpoint(String endpoint) {
      this.endpoint = endpoint;
    }

    public String getAccessKey() {
      return accessKey;
    }

    public void setAccessKey(String accessKey) {
      this.accessKey = accessKey;
    }

    public String getSecretKey() {
      return secretKey;
    }

    public void setSecretKey(String secretKey) {
      this.secretKey = secretKey;
    }
  }
}
