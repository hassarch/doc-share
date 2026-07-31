package com.docshare.backend.notification.repository;

import com.docshare.backend.notification.entity.Notification;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

  List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId);

  long countByUserIdAndReadFalse(UUID userId);
}
