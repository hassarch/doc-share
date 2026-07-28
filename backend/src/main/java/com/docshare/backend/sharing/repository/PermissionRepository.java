package com.docshare.backend.sharing.repository;

import com.docshare.backend.sharing.entity.Permission;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionRepository extends JpaRepository<Permission, UUID> {

  List<Permission> findByDocumentId(UUID documentId);

  List<Permission> findByUserId(UUID userId);

  Optional<Permission> findByDocumentIdAndUserId(UUID documentId, UUID userId);
}
