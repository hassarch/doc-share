package com.docshare.backend.sharing.controller;

import com.docshare.backend.sharing.dto.CreateShareRequest;
import com.docshare.backend.sharing.dto.ShareResponse;
import com.docshare.backend.sharing.service.SharingService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Direct sharing REST API - share documents with users by email, list shares, revoke access. */
@RestController
@RequestMapping("/api/v1")
public class ShareController {

  private final SharingService sharingService;

  public ShareController(SharingService sharingService) {
    this.sharingService = sharingService;
  }

  /**
   * Share a document with a user by email.
   *
   * <p>POST /api/v1/shares
   *
   * <p>Request body: { "documentId": "uuid", "email": "user@example.com", "role": "VIEWER" }
   */
  @PostMapping("/shares")
  public ResponseEntity<ShareResponse> createShare(
      @Valid @RequestBody CreateShareRequest request, @AuthenticationPrincipal String userId) {
    ShareResponse response =
        sharingService.createShare(
            UUID.fromString(userId), request.documentId(), request.email(), request.role());
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * List all shares for a document.
   *
   * <p>GET /api/v1/documents/{documentId}/shares
   */
  @GetMapping("/documents/{documentId}/shares")
  public ResponseEntity<List<ShareResponse>> listShares(
      @PathVariable UUID documentId, @AuthenticationPrincipal String userId) {
    List<ShareResponse> shares = sharingService.listShares(UUID.fromString(userId), documentId);
    return ResponseEntity.ok(shares);
  }

  /**
   * Revoke access to a document.
   *
   * <p>DELETE /api/v1/shares/{shareId}
   */
  @DeleteMapping("/shares/{shareId}")
  public ResponseEntity<Void> revokeShare(
      @PathVariable UUID shareId, @AuthenticationPrincipal String userId) {
    sharingService.revokeShare(UUID.fromString(userId), shareId);
    return ResponseEntity.noContent().build();
  }
}
