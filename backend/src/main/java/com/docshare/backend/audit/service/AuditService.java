package com.docshare.backend.audit.service;

import com.docshare.backend.audit.entity.AuditLogEntry;
import com.docshare.backend.audit.entity.AuditResult;
import com.docshare.backend.audit.repository.AuditLogRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records immutable audit entries (FR-13.1-13.4). Called by {@link
 * com.docshare.backend.audit.consumer.AuditEventConsumer}, kept as its own service (rather than
 * inlining the repository call in the consumer) so a future admin-facing "view audit log" endpoint
 * (FR-13.4) can reuse this same write path's query methods without duplicating logic.
 */
@Service
public class AuditService {

  private final AuditLogRepository auditLogRepository;

  public AuditService(AuditLogRepository auditLogRepository) {
    this.auditLogRepository = auditLogRepository;
  }

  @Transactional
  public void record(
      UUID actorId, String action, String targetType, UUID targetId, String metadataJson) {
    auditLogRepository.save(
        new AuditLogEntry(
            actorId, action, targetType, targetId, AuditResult.SUCCESS, metadataJson));
  }
}
