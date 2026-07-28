package com.docshare.backend.documents.repository;

import com.docshare.backend.documents.entity.Folder;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FolderRepository extends JpaRepository<Folder, UUID> {

  List<Folder> findByOwnerId(UUID ownerId);

  List<Folder> findByParentFolderId(UUID parentFolderId);
}
