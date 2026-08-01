package com.docshare.backend.sharing.dto;

import com.docshare.backend.sharing.entity.Permission;
import com.docshare.backend.sharing.entity.PermissionRole;
import java.time.Instant;
import java.util.UUID;

public record ShareResponse(
    UUID id,
    UUID documentId,
    UUID userId,
    String userEmail,
    String userName,
    PermissionRole role,
    Instant grantedAt) {

  public static ShareResponse from(Permission permission, String userEmail, String userName) {
    return new ShareResponse(
        permission.getId(),
        permission.getDocumentId(),
        permission.getUserId(),
        userEmail,
        userName,
        permission.getRole(),
        permission.getGrantedAt());
  }
}
