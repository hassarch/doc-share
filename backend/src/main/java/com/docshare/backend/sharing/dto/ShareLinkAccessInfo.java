package com.docshare.backend.sharing.dto;

import java.util.UUID;

public record ShareLinkAccessInfo(
    UUID documentId, String filename, boolean canDownload, boolean isExpired) {}
