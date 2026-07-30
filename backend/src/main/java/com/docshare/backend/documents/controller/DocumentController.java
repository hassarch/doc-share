package com.docshare.backend.documents.controller;

import com.docshare.backend.common.exception.ValidationException;
import com.docshare.backend.common.util.CurrentUser;
import com.docshare.backend.documents.dto.DocumentResponse;
import com.docshare.backend.documents.dto.MoveDocumentRequest;
import com.docshare.backend.documents.dto.RenameDocumentRequest;
import com.docshare.backend.documents.entity.Document;
import com.docshare.backend.documents.service.DocumentService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

  // FR-3.10: supported file types at launch. Enforced here (before the
  // service layer or storage even sees the bytes) since this is
  // request-shape validation, not business logic.
  private static final List<String> ALLOWED_MIME_TYPES =
      List.of(
          "application/pdf",
          "image/jpeg",
          "image/png",
          "image/gif",
          "application/msword",
          "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
          "application/vnd.ms-excel",
          "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
          "application/zip",
          "text/plain");

  private final DocumentService documentService;

  public DocumentController(DocumentService documentService) {
    this.documentService = documentService;
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<DocumentResponse> upload(
      @RequestPart("file") MultipartFile file,
      @RequestParam(required = false) UUID folderId)
      throws IOException {
    if (file.isEmpty()) {
      throw new ValidationException("Uploaded file is empty");
    }
    String contentType = file.getContentType();
    if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
      throw new ValidationException("Unsupported file type: " + contentType);
    }

    Document document =
        documentService.upload(
            CurrentUser.id(), folderId, file.getOriginalFilename(), contentType, file.getBytes());
    return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(document));
  }

  @GetMapping
  public ResponseEntity<List<DocumentResponse>> list(
      @RequestParam(required = false) UUID folderId) {
    List<DocumentResponse> documents =
        documentService.list(CurrentUser.id(), folderId).stream()
            .map(DocumentController::toResponse)
            .toList();
    return ResponseEntity.ok(documents);
  }

  @GetMapping("/{id}")
  public ResponseEntity<DocumentResponse> get(@PathVariable UUID id) {
    Document document = documentService.getOwned(id, CurrentUser.id());
    return ResponseEntity.ok(toResponse(document));
  }

  /**
   * Streams the file directly rather than issuing a signed URL — signed,
   * time-limited MinIO URLs (FR-3.2/FR-19.8) are a small addition once the
   * frontend needs to embed direct links (e.g. for in-browser PDF
   * preview); a straightforward authenticated stream is the right default
   * for a basic download button today.
   */
  @GetMapping("/{id}/download")
  public ResponseEntity<InputStreamResource> download(@PathVariable UUID id) {
    Document document = documentService.getOwned(id, CurrentUser.id());
    InputStream content = documentService.download(id, CurrentUser.id());

    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(document.getMimeType()))
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + document.getFilename() + "\"")
        .contentLength(document.getSizeBytes())
        .body(new InputStreamResource(content));
  }

  @PatchMapping("/{id}")
  public ResponseEntity<DocumentResponse> rename(
      @PathVariable UUID id, @Valid @RequestBody RenameDocumentRequest request) {
    Document document = documentService.rename(id, CurrentUser.id(), request.filename());
    return ResponseEntity.ok(toResponse(document));
  }

  @PatchMapping("/{id}/move")
  public ResponseEntity<DocumentResponse> move(
      @PathVariable UUID id, @RequestBody MoveDocumentRequest request) {
    Document document = documentService.move(id, CurrentUser.id(), request.folderId());
    return ResponseEntity.ok(toResponse(document));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    documentService.softDelete(id, CurrentUser.id());
    return ResponseEntity.noContent().build();
  }

  private static DocumentResponse toResponse(Document document) {
    return new DocumentResponse(
        document.getId(),
        document.getFilename(),
        document.getFolderId(),
        document.getSizeBytes(),
        document.getMimeType(),
        document.getSha256Hash(),
        document.getReplicationStatus().name(),
        document.getCreatedAt(),
        document.getUpdatedAt());
  }
}
