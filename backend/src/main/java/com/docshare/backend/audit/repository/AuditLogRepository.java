package com.docshare.backend.audit.repository;

import com.docshare.backend.audit.entity.AuditLogEntry;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLogEntry, UUID> {

  List<AuditLogEntry> findByActorIdOrderByOccurredAtDesc(UUID actorId);

  List<AuditLogEntry> findByTargetTypeAndTargetIdOrderByOccurredAtDesc(
      String targetType, UUID targetId);
}
