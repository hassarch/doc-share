/**
 * Auth module — registration, login, JWT issuance/refresh, password reset.
 *
 * <p>Future extraction target: Auth Service (Phase 3+, per the PRD).
 *
 * <p>Boundary rule (ADR-0001 #2): other modules may call into {@code auth.service} interfaces, but
 * must never reach into {@code auth.repository} directly.
 */
package com.docshare.backend.auth;
