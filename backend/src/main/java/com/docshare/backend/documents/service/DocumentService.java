package com.docshare.backend.documents.service;

import com.docshare.backend.common.exception.NotFoundException;
import com.docshare.backend.common.exception.ValidationException;
import com.docshare.backend.documents.entity.Document;
import com.docshare.backend.documents.entity.DocumentVersion;
import com.docshare.backend.documents.entity.Folder;
import com.docshare.backend.documents.repository.DocumentRepository;
import com.docshare.backend.documents.repository.DocumentVersionRepository;
import com.docshare.backend.documents.repository.FolderRepository;
import com.docshare.backend.storage.service.StorageService;
import com.docshare.backend.users.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates document upload/download/rename/move/delete.
 *
 * <p><strong>Authorization scope:</strong> every method here checks ownership only. Full RBAC via
 * {@code sharing.entity.Permission} (Viewer/Commenter/Editor/Owner, FR-1.8/4.2) is not wired in yet
 * — that's the Sharing module's job, deliberately not folded into this phase. A document a Viewer
 * has been granted access to will currently 404 for them, which is a known, temporary gap, not a
 * design decision to leave permanently.
 */
@Service
public class DocumentService {

  private final DocumentRepository documentRepository;
  private final DocumentVersionRepository documentVersionRepository;
  private final FolderRepository folderRepository;
  private final StorageService storageService;
  private final UserService userService;
  private final ObjectMapper objectMapper;

  public DocumentService(
      DocumentRepository documentRepository,
      DocumentVersionRepository documentVersionRepository,
      FolderRepository folderRepository,
      StorageService storageService,
      UserService userService,
      ObjectMapper objectMapper) {
    this.documentRepository = documentRepository;
    this.documentVersionRepository = documentVersionRepository;
    this.folderRepository = folderRepository;
    this.storageService = storageService;
    this.userService = userService;
    this.objectMapper = objectMapper;
  }

  /**
   * Uploads a new document (FR-3.1). Hashes the content first and checks for an existing match
   * (FR-9.2 dedup) before touching storage — see this class's Javadoc header comment in the Phase 6
   * write-up for why that ordering matters and why hashing reads the whole file into memory for now
   * (acceptable at this phase's scale; streaming hashing is a Chunking-phase concern for large
   * files).
   */
  @Transactional
  public Document upload(
      UUID ownerId, UUID folderId, String filename, String contentType, byte[] content) {
    if (folderId != null) {
      // Ownership check without needing the Folder object further —
      // throws NotFoundException if the folder doesn't belong to ownerId.
      folderRepository
          .findById(folderId)
          .filter(f -> f.getOwnerId().equals(ownerId))
          .orElseThrow(() -> new NotFoundException("Folder not found"));
    }

    String sha256Hash = sha256Hex(content);

    var existingMatch = documentRepository.findFirstBySha256HashAndDeletedFalse(sha256Hash);

    Document document =
        new Document(ownerId, filename, folderId, content.length, contentType, sha256Hash);
    document = documentRepository.save(document);

    String objectKey;
    if (existingMatch.isPresent()) {
      // FR-9.2: reuse the existing physical bytes — no new upload, no
      // extra storage consumed for this user's quota either, since
      // nothing new was actually written.
      StorageRef existingRef = readStorageRef(existingMatch.get().getCurrentVersion());
      objectKey = existingRef.objectKey();
    } else {
      objectKey = UUID.randomUUID().toString();
      storageService.upload(
          objectKey, new java.io.ByteArrayInputStream(content), content.length, contentType);
      userService.recordStorageUsage(ownerId, content.length);
    }

    DocumentVersion version =
        new DocumentVersion(
            document, 1, content.length, sha256Hash, ownerId, writeStorageRef(objectKey));
    version = documentVersionRepository.save(version);

    document.setCurrentVersion(version);
    return documentRepository.save(document);
  }

  public Document getOwned(UUID documentId, UUID requesterId) {
    Document document =
        documentRepository
            .findById(documentId)
            .filter(d -> !d.isDeleted())
            .orElseThrow(() -> new NotFoundException("Document not found"));
    if (!document.getOwnerId().equals(requesterId)) {
      // Same reasoning as FolderService: NotFoundException, not
      // ForbiddenException, to avoid confirming existence to a non-owner.
      throw new NotFoundException("Document not found");
    }
    return document;
  }

  public List<Document> list(UUID ownerId, UUID folderId) {
    return folderId == null
        ? documentRepository.findByOwnerIdAndDeletedFalse(ownerId)
        : documentRepository.findByFolderIdAndDeletedFalse(folderId);
  }

  /** Returns the raw content stream for a document's current version. */
  public InputStream download(UUID documentId, UUID requesterId) {
    Document document = getOwned(documentId, requesterId);
    StorageRef ref = readStorageRef(document.getCurrentVersion());
    return storageService.download(ref.objectKey());
  }

  @Transactional
  public Document rename(UUID documentId, UUID requesterId, String newFilename) {
    Document document = getOwned(documentId, requesterId);
    document.rename(newFilename);
    return documentRepository.save(document);
  }

  @Transactional
  public Document move(UUID documentId, UUID requesterId, UUID newFolderId) {
    Document document = getOwned(documentId, requesterId);
    if (newFolderId != null) {
      Folder targetFolder =
          folderRepository
              .findById(newFolderId)
              .orElseThrow(() -> new NotFoundException("Folder not found"));
      if (!targetFolder.getOwnerId().equals(requesterId)) {
        throw new NotFoundException("Folder not found");
      }
    }
    document.moveTo(newFolderId);
    return documentRepository.save(document);
  }

  /** FR-3.3: soft delete first — the hard-delete background job is a later phase. */
  @Transactional
  public void softDelete(UUID documentId, UUID requesterId) {
    Document document = getOwned(documentId, requesterId);
    document.softDelete();
    documentRepository.save(document);
    userService.recordStorageUsage(requesterId, -document.getSizeBytes());
    // Physical bytes are intentionally NOT deleted from storage here —
    // per FR-9.4, reference counting must confirm no other document
    // metadata record still points at the same physical object first.
    // That reconciliation is a Deduplication/retention-phase concern.
  }

  private String writeStorageRef(String objectKey) {
    try {
      return objectMapper.writeValueAsString(StorageRef.single(objectKey));
    } catch (IOException e) {
      throw new ValidationException("Failed to serialize storage reference");
    }
  }

  private StorageRef readStorageRef(DocumentVersion version) {
    try {
      return objectMapper.readValue(version.getStorageRef(), StorageRef.class);
    } catch (IOException e) {
      throw new ValidationException("Failed to parse storage reference");
    }
  }

  private static String sha256Hex(byte[] content) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(content));
    } catch (NoSuchAlgorithmException e) {
      // SHA-256 is a mandatory JDK algorithm; this can't actually happen
      // on any conforming JVM, but the checked exception still needs a
      // home.
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }
}
