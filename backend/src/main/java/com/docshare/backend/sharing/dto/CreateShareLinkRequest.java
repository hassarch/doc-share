package com.docshare.backend.sharing.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.util.UUID;

public record CreateShareLinkRequest(
    @NotNull UUID documentId,
    Instant expiresAt,
    String password,
    @Positive Integer downloadLimit,
    boolean readOnly) {}
