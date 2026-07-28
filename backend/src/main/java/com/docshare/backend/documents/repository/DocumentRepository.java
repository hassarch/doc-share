package com.docshare.backend.documents.repository;

import com.docshare.backend.documents.entity.Document;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

  List<Document> findByOwnerIdAndDeletedFalse(UUID ownerId);

  List<Document> findByFolderIdAndDeletedFalse(UUID folderId);

  // Backs deduplication (FR-9.2): find any existing document with matching
  // content hash before writing new bytes.
  Optional<Document> findFirstBySha256HashAndDeletedFalse(String sha256Hash);
}
