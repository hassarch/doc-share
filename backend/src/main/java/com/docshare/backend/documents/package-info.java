/**
 * Documents module — upload/download orchestration, metadata CRUD, folder structure, versioning,
 * deduplication.
 *
 * <p>This is the core module and stays part of the backend rather than being extracted into its own
 * service.
 *
 * <p>Boundary rule (ADR-0001 #2): other modules may call into {@code documents.service} interfaces,
 * but must never reach into {@code documents.repository} directly.
 */
package com.docshare.backend.documents;
