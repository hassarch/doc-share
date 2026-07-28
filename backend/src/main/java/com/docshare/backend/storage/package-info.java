/**
 * Storage module — abstraction over physical storage nodes (MinIO), chunking, checksum
 * verification, replication.
 *
 * <p>Future extraction target: Storage Service (Phase 1, per the PRD — the first service to be
 * split out of the monolith). All other modules access files only via {@code storage.service}
 * interfaces, referencing documents by ID — never by physical location.
 */
package com.docshare.backend.storage;
