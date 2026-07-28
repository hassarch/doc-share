package com.docshare.backend.documents.repository;

import com.docshare.backend.documents.entity.Chunk;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChunkRepository extends JpaRepository<Chunk, UUID> {

  List<Chunk> findByDocumentVersionIdOrderByChunkNumberAsc(UUID documentVersionId);
}
