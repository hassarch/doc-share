package com.docshare.backend.sharing.service;

import com.docshare.backend.common.exception.ConflictException;
import com.docshare.backend.common.exception.ForbiddenException;
import com.docshare.backend.common.exception.NotFoundException;
import com.docshare.backend.documents.entity.Document;
import com.docshare.backend.documents.repository.DocumentRepository;
import com.docshare.backend.sharing.dto.ShareResponse;
import com.docshare.backend.sharing.entity.Permission;
import com.docshare.backend.sharing.entity.PermissionRole;
import com.docshare.backend.sharing.repository.PermissionRepository;
import com.docshare.backend.users.entity.User;
import com.docshare.backend.users.service.UserService;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Direct sharing service - share documents with specific users by email with role-based
 * permissions.
 */
@Service
public class SharingService {

  private final PermissionRepository permissionRepository;
  private final DocumentRepository documentRepository;
  private final UserService userService;

  public SharingService(
      PermissionRepository permissionRepository,
      DocumentRepository documentRepository,
      UserService userService) {
    this.permissionRepository = permissionRepository;
    this.documentRepository = documentRepository;
    this.userService = userService;
  }

  /**
   * Share a document with a user by email. Only the document owner can share.
   *
   * @throws NotFoundException if document or user not found
   * @throws ForbiddenException if requester is not the document owner
   * @throws ConflictException if permission already exists
   */
  @Transactional
  public ShareResponse createShare(
      UUID requesterId, UUID documentId, String email, PermissionRole role) {
    // Verify document exists and requester is owner
    Document document =
        documentRepository
            .findById(documentId)
            .orElseThrow(() -> new NotFoundException("Document not found"));

    if (!document.getOwnerId().equals(requesterId)) {
      throw new ForbiddenException("Only the document owner can share it");
    }

    // Find user by email
    User targetUser =
        userService
            .findByEmail(email)
            .orElseThrow(() -> new NotFoundException("User with email " + email + " not found"));

    // Prevent sharing with self
    if (targetUser.getId().equals(requesterId)) {
      throw new ConflictException("Cannot share document with yourself");
    }

    // Check if permission already exists
    if (permissionRepository
        .findByDocumentIdAndUserId(documentId, targetUser.getId())
        .isPresent()) {
      throw new ConflictException("Document already shared with this user");
    }

    // Create permission
    Permission permission = new Permission(documentId, targetUser.getId(), role, requesterId);
    permission = permissionRepository.save(permission);

    return ShareResponse.from(permission, targetUser.getEmail(), targetUser.getName());
  }

  /**
   * List all shares for a document. Only the document owner can list shares.
   *
   * @throws NotFoundException if document not found
   * @throws ForbiddenException if requester is not the document owner
   */
  @Transactional(readOnly = true)
  public List<ShareResponse> listShares(UUID requesterId, UUID documentId) {
    // Verify document exists and requester is owner
    Document document =
        documentRepository
            .findById(documentId)
            .orElseThrow(() -> new NotFoundException("Document not found"));

    if (!document.getOwnerId().equals(requesterId)) {
      throw new ForbiddenException("Only the document owner can list shares");
    }

    List<Permission> permissions = permissionRepository.findByDocumentId(documentId);

    return permissions.stream()
        .map(
            perm -> {
              User user =
                  userService
                      .findById(perm.getUserId())
                      .orElseThrow(() -> new NotFoundException("User not found"));
              return ShareResponse.from(perm, user.getEmail(), user.getName());
            })
        .toList();
  }

  /**
   * Revoke a share. Only the document owner or the granted user can revoke.
   *
   * @throws NotFoundException if permission not found
   * @throws ForbiddenException if requester is not authorized
   */
  @Transactional
  public void revokeShare(UUID requesterId, UUID shareId) {
    Permission permission =
        permissionRepository
            .findById(shareId)
            .orElseThrow(() -> new NotFoundException("Share not found"));

    // Get document to check ownership
    Document document =
        documentRepository
            .findById(permission.getDocumentId())
            .orElseThrow(() -> new NotFoundException("Document not found"));

    // Only owner or the shared user can revoke
    if (!document.getOwnerId().equals(requesterId) && !permission.getUserId().equals(requesterId)) {
      throw new ForbiddenException("Not authorized to revoke this share");
    }

    permissionRepository.delete(permission);
  }

  /** List documents shared with a user. */
  @Transactional(readOnly = true)
  public List<Document> listSharedWithUser(UUID userId) {
    List<Permission> permissions = permissionRepository.findByUserId(userId);

    return permissions.stream()
        .map(Permission::getDocumentId)
        .map(documentRepository::findById)
        .filter(java.util.Optional::isPresent)
        .map(java.util.Optional::get)
        .toList();
  }
}
