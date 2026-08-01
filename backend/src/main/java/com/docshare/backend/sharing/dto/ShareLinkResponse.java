package com.docshare.backend.sharing.dto;

import com.docshare.backend.sharing.entity.ShareLink;
import java.time.Instant;
import java.util.UUID;

public record ShareLinkResponse(
    UUID id,
    UUID documentId,
    String token,
    Instant expiresAt,
    boolean hasPassword,
    Integer downloadLimit,
    int downloadsUsed,
    boolean readOnly,
    Instant createdAt) {

  public static ShareLinkResponse from(ShareLink link) {
    return new ShareLinkResponse(
        link.getId(),
        link.getDocumentId(),
        link.getToken(),
        link.getExpiresAt(),
        link.getPasswordHash() != null,
        link.getDownloadLimit(),
        link.getDownloadsUsed(),
        link.isReadOnly(),
        link.getCreatedAt());
  }
}
