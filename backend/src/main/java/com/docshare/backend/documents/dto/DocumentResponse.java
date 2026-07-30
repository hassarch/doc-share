package com.docshare.backend.documents.dto;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
    UUID id,
    String filename,
    UUID folderId,
    long sizeBytes,
    String mimeType,
    String sha256Hash,
    String replicationStatus,
    Instant createdAt,
    Instant updatedAt) {}
