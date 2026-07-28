package com.docshare.backend.storage.repository;

import com.docshare.backend.storage.entity.StorageNode;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StorageNodeRepository extends JpaRepository<StorageNode, UUID> {

  Optional<StorageNode> findByName(String name);
}
