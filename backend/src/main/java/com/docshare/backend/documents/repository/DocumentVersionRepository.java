package com.docshare.backend.documents.repository;

import com.docshare.backend.documents.entity.DocumentVersion;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, UUID> {

  List<DocumentVersion> findByDocumentIdOrderByVersionNumberDesc(UUID documentId);
}
