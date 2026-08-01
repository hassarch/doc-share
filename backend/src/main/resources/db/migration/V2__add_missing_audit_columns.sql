-- V2__add_missing_audit_columns.sql
-- Add created_at and updated_at columns to tables that were missing them.
-- All entities extend BaseEntity which requires these audit columns.
-- 
-- Tables that already have both columns (no changes needed):
--   - users, folders, storage_nodes, documents, notifications
--
-- Tables that need fixing:
--   - chunks: missing both created_at and updated_at
--   - document_versions: has created_at, missing updated_at
--   - permissions: has granted_at but needs created_at/updated_at
--   - audit_log: has occurred_at but needs created_at/updated_at
--   - share_links: has created_at, missing updated_at

ALTER TABLE chunks 
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

ALTER TABLE document_versions
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

ALTER TABLE permissions
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

ALTER TABLE audit_log
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

ALTER TABLE share_links
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();
