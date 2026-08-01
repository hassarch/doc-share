package com.docshare.backend.sharing.controller;

import com.docshare.backend.common.exception.NotFoundException;
import com.docshare.backend.common.exception.ValidationException;
import com.docshare.backend.common.util.CurrentUser;
import com.docshare.backend.documents.entity.Document;
import com.docshare.backend.documents.entity.DocumentVersion;
import com.docshare.backend.documents.repository.DocumentRepository;
import com.docshare.backend.documents.service.StorageRef;
import com.docshare.backend.sharing.dto.AccessShareLinkRequest;
import com.docshare.backend.sharing.dto.CreateShareLinkRequest;
import com.docshare.backend.sharing.dto.ShareLinkAccessInfo;
import com.docshare.backend.sharing.dto.ShareLinkResponse;
import com.docshare.backend.sharing.service.ShareLinkService;
import com.docshare.backend.storage.service.StorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public share link REST API - create password-protected, expiring links for external sharing. */
@RestController
@RequestMapping("/api/v1")
public class ShareLinkController {

  private final ShareLinkService shareLinkService;
  private final DocumentRepository documentRepository;
  private final StorageService storageService;
  private final ObjectMapper objectMapper;

  public ShareLinkController(
      ShareLinkService shareLinkService,
      DocumentRepository documentRepository,
      StorageService storageService,
      ObjectMapper objectMapper) {
    this.shareLinkService = shareLinkService;
    this.documentRepository = documentRepository;
    this.storageService = storageService;
    this.objectMapper = objectMapper;
  }

  /**
   * Create a public share link (authenticated).
   *
   * <p>POST /api/v1/share-links
   *
   * <p>Request body: { "documentId": "uuid", "expiresAt": "2026-12-31T23:59:59Z", "password":
   * "optional", "downloadLimit": 10, "readOnly": true }
   */
  @PostMapping("/share-links")
  public ResponseEntity<ShareLinkResponse> createShareLink(
      @Valid @RequestBody CreateShareLinkRequest request) {
    ShareLinkResponse response = shareLinkService.createShareLink(CurrentUser.id(), request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * List share links for a document (authenticated).
   *
   * <p>GET /api/v1/documents/{documentId}/share-links
   */
  @GetMapping("/documents/{documentId}/share-links")
  public ResponseEntity<List<ShareLinkResponse>> listShareLinks(@PathVariable UUID documentId) {
    List<ShareLinkResponse> links = shareLinkService.listShareLinks(CurrentUser.id(), documentId);
    return ResponseEntity.ok(links);
  }

  /**
   * Delete a share link (authenticated).
   *
   * <p>DELETE /api/v1/share-links/{linkId}
   */
  @DeleteMapping("/share-links/{linkId}")
  public ResponseEntity<Void> deleteShareLink(@PathVariable UUID linkId) {
    shareLinkService.deleteShareLink(CurrentUser.id(), linkId);
    return ResponseEntity.noContent().build();
  }

  /**
   * Access a share link (PUBLIC - no authentication required).
   *
   * <p>POST /api/v1/share-links/{token}
   *
   * <p>Request body (optional): { "password": "if-required" }
   *
   * <p>Returns document info if access is granted.
   */
  @PostMapping("/share-links/{token}")
  public ResponseEntity<ShareLinkAccessInfo> accessShareLink(
      @PathVariable String token, @RequestBody(required = false) AccessShareLinkRequest request) {
    String password = request != null ? request.password() : null;
    ShareLinkAccessInfo info = shareLinkService.accessShareLink(token, password);
    return ResponseEntity.ok(info);
  }

  /**
   * Download via share link (PUBLIC - no authentication required).
   *
   * <p>POST /api/v1/share-links/{token}/download
   *
   * <p>Request body (optional): { "password": "if-required" }
   */
  @PostMapping("/share-links/{token}/download")
  public ResponseEntity<Resource> downloadViaShareLink(
      @PathVariable String token, @RequestBody(required = false) AccessShareLinkRequest request)
      throws IOException {
    String password = request != null ? request.password() : null;

    // Validate access first
    ShareLinkAccessInfo info = shareLinkService.accessShareLink(token, password);

    if (!info.canDownload()) {
      throw new NotFoundException("Download not available");
    }

    // Get document
    Document document =
        documentRepository
            .findById(info.documentId())
            .orElseThrow(() -> new NotFoundException("Document not found"));

    // Record download
    shareLinkService.recordDownload(token);

    // Get file from storage - same logic as DocumentService.download
    DocumentVersion currentVersion = document.getCurrentVersion();
    StorageRef ref = readStorageRef(currentVersion);
    InputStream fileStream = storageService.download(ref.objectKey());

    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(document.getMimeType()))
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + document.getFilename() + "\"")
        .body(new InputStreamResource(fileStream));
  }

  private StorageRef readStorageRef(DocumentVersion version) {
    try {
      return objectMapper.readValue(version.getStorageRef(), StorageRef.class);
    } catch (IOException e) {
      throw new ValidationException("Failed to parse storage reference");
    }
  }
}
