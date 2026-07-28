/**
 * Users module — profiles, settings, storage quota tracking.
 *
 * <p>Future extraction target: User Service (Phase 3+, per the PRD).
 *
 * <p>Boundary rule (ADR-0001 #2): other modules may call into {@code users.service} interfaces, but
 * must never reach into {@code users.repository} directly.
 */
package com.docshare.backend.users;
