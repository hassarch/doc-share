package com.docshare.backend.sharing.service;

import com.docshare.backend.common.exception.ForbiddenException;
import com.docshare.backend.common.exception.InvalidCredentialsException;
import com.docshare.backend.common.exception.NotFoundException;
import com.docshare.backend.documents.entity.Document;
import com.docshare.backend.documents.repository.DocumentRepository;
import com.docshare.backend.sharing.dto.CreateShareLinkRequest;
import com.docshare.backend.sharing.dto.ShareLinkAccessInfo;
import com.docshare.backend.sharing.dto.ShareLinkResponse;
import com.docshare.backend.sharing.entity.ShareLink;
import com.docshare.backend.sharing.repository.ShareLinkRepository;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Public share link service - create password-protected, expiring, download-limited links for
 * external sharing.
 */
@Service
public class ShareLinkService {

  private final ShareLinkRepository shareLinkRepository;
  private final DocumentRepository documentRepository;
  private final PasswordEncoder passwordEncoder;
  private final SecureRandom secureRandom;

  public ShareLinkService(
      ShareLinkRepository shareLinkRepository,
      DocumentRepository documentRepository,
      PasswordEncoder passwordEncoder) {
    this.shareLinkRepository = shareLinkRepository;
    this.documentRepository = documentRepository;
    this.passwordEncoder = passwordEncoder;
    this.secureRandom = new SecureRandom();
  }

  /**
   * Create a public share link. Only document owner can create links.
   *
   * @throws NotFoundException if document not found
   * @throws ForbiddenException if requester is not the document owner
   */
  @Transactional
  public ShareLinkResponse createShareLink(UUID requesterId, CreateShareLinkRequest request) {
    // Verify document exists and requester is owner
    Document document =
        documentRepository
            .findById(request.documentId())
            .orElseThrow(() -> new NotFoundException("Document not found"));

    if (!document.getOwnerId().equals(requesterId)) {
      throw new ForbiddenException("Only the document owner can create share links");
    }

    // Generate secure random token
    String token = generateToken();

    // Hash password if provided
    String passwordHash = null;
    if (request.password() != null && !request.password().isBlank()) {
      passwordHash = passwordEncoder.encode(request.password());
    }

    // Create share link
    ShareLink shareLink =
        new ShareLink(
            request.documentId(),
            token,
            request.expiresAt(),
            passwordHash,
            request.downloadLimit(),
            request.readOnly());

    shareLink = shareLinkRepository.save(shareLink);

    return ShareLinkResponse.from(shareLink);
  }

  /**
   * List share links for a document. Only document owner can list.
   *
   * @throws NotFoundException if document not found
   * @throws ForbiddenException if requester is not the document owner
   */
  @Transactional(readOnly = true)
  public List<ShareLinkResponse> listShareLinks(UUID requesterId, UUID documentId) {
    // Verify document exists and requester is owner
    Document document =
        documentRepository
            .findById(documentId)
            .orElseThrow(() -> new NotFoundException("Document not found"));

    if (!document.getOwnerId().equals(requesterId)) {
      throw new ForbiddenException("Only the document owner can list share links");
    }

    return shareLinkRepository.findByDocumentId(documentId).stream()
        .map(ShareLinkResponse::from)
        .toList();
  }

  /**
   * Delete a share link. Only document owner can delete.
   *
   * @throws NotFoundException if link not found
   * @throws ForbiddenException if requester is not the document owner
   */
  @Transactional
  public void deleteShareLink(UUID requesterId, UUID linkId) {
    ShareLink shareLink =
        shareLinkRepository
            .findById(linkId)
            .orElseThrow(() -> new NotFoundException("Share link not found"));

    // Get document to check ownership
    Document document =
        documentRepository
            .findById(shareLink.getDocumentId())
            .orElseThrow(() -> new NotFoundException("Document not found"));

    if (!document.getOwnerId().equals(requesterId)) {
      throw new ForbiddenException("Only the document owner can delete share links");
    }

    shareLinkRepository.delete(shareLink);
  }

  /**
   * Access a share link (public endpoint, no authentication required). Validates password if
   * required and checks expiry/download limits.
   *
   * @throws NotFoundException if link not found
   * @throws InvalidCredentialsException if password is incorrect
   * @throws ForbiddenException if link is expired or download limit reached
   */
  @Transactional(readOnly = true)
  public ShareLinkAccessInfo accessShareLink(String token, String password) {
    ShareLink shareLink =
        shareLinkRepository
            .findByToken(token)
            .orElseThrow(() -> new NotFoundException("Share link not found"));

    // Check expiry
    if (shareLink.isExpired()) {
      return new ShareLinkAccessInfo(
          shareLink.getDocumentId(), null, false, true); // isExpired = true
    }

    // Check password
    if (shareLink.getPasswordHash() != null) {
      if (password == null || !passwordEncoder.matches(password, shareLink.getPasswordHash())) {
        throw new InvalidCredentialsException("Invalid password");
      }
    }

    // Check download limit
    boolean canDownload = !shareLink.isDownloadLimitReached();

    // Get document info
    Document document =
        documentRepository
            .findById(shareLink.getDocumentId())
            .orElseThrow(() -> new NotFoundException("Document not found"));

    return new ShareLinkAccessInfo(document.getId(), document.getFilename(), canDownload, false);
  }

  /**
   * Record a download for a share link (increments download counter).
   *
   * @throws NotFoundException if link not found
   * @throws ForbiddenException if download limit reached or expired
   */
  @Transactional
  public void recordDownload(String token) {
    ShareLink shareLink =
        shareLinkRepository
            .findByToken(token)
            .orElseThrow(() -> new NotFoundException("Share link not found"));

    if (shareLink.isExpired()) {
      throw new ForbiddenException("Share link has expired");
    }

    if (shareLink.isDownloadLimitReached()) {
      throw new ForbiddenException("Download limit reached");
    }

    shareLink.recordDownload();
    shareLinkRepository.save(shareLink);
  }

  /** Generate a secure random token for share links (URL-safe base64, 32 bytes = 256 bits). */
  private String generateToken() {
    byte[] bytes = new byte[32];
    secureRandom.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }
}
