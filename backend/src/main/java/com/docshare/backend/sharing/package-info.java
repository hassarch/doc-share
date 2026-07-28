/**
 * Sharing module — permissions, share links, group sharing (future).
 *
 * <p>Future extraction target: Sharing Service (Phase 3+, per the PRD).
 *
 * <p>Boundary rule (ADR-0001 #2): other modules may call into {@code sharing.service} interfaces,
 * but must never reach into {@code sharing.repository} directly.
 */
package com.docshare.backend.sharing;
