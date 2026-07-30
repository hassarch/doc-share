package com.docshare.backend.documents.dto;

import java.util.UUID;

/** {@code folderId} is nullable — moving to root clears it. */
public record MoveDocumentRequest(UUID folderId) {}
