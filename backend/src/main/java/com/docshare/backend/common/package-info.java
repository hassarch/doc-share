/**
 * Common module — shared DTOs, exception types, a shared base entity, and stateless utilities used
 * across other modules.
 *
 * <p>Deliberately does NOT contain domain business logic, domain entities, or repositories — those
 * belong to a specific domain module. {@code common.entity} holds only the
 * {@code @MappedSuperclass} shared by domain entities (auditing fields), never a domain entity
 * itself. If you're tempted to put a service class or a domain entity (User, Document, Folder, ...)
 * here, it belongs in a domain module instead.
 */
package com.docshare.backend.common;
