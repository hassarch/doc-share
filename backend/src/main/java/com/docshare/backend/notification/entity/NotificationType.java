package com.docshare.backend.notification.entity;

/**
 * FR-12.1-12.5 lists five triggers; this phase implements the one {@code
 * documents.event.DocumentEventPublisher} actually produces today (upload completion, FR-12.4 —
 * "relevant for large/chunked uploads", though it fires for every upload here for simplicity). The
 * others (share, comment, edit, quota) get their own enum values the moment their triggering
 * feature exists — adding a value here is a one-line change, not a redesign.
 */
public enum NotificationType {
  UPLOAD_COMPLETE
}
