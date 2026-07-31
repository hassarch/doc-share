package com.docshare.backend.notification.dto;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
    UUID id, String type, String payloadJson, boolean read, Instant createdAt) {}
