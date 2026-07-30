package com.docshare.backend.documents.dto;

import java.time.Instant;
import java.util.UUID;

public record FolderResponse(
    UUID id, String name, UUID parentFolderId, Instant createdAt, Instant updatedAt) {}
