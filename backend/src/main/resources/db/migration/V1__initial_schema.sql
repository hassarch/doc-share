-- V1__initial_schema.sql
-- Initial schema per PRD Section 7 (Data Model).
--
-- Notes:
--   * All primary keys are UUIDs, generated application-side by Hibernate
--     (see BaseEntity) — no DB-side default needed.
--   * created_at/updated_at columns back the shared BaseEntity auditing
--     fields (populated by Spring Data JPA auditing, not DB triggers).
--   * audit_log and notifications tables are created now (schema-first)
--     but have NO JPA entity yet — those modules don't exist as Java
--     packages until the Events/Notifications/Audit phase. Nothing else
--     in this migration depends on that; it's just cheaper to lay down
--     once than to migrate again later.

-- ============================================================
-- users
-- ============================================================
CREATE TABLE users (
    id                   UUID PRIMARY KEY,
    email                VARCHAR(255) NOT NULL,
    password_hash        VARCHAR(255) NOT NULL,
    name                 VARCHAR(255) NOT NULL,
    storage_quota_bytes  BIGINT NOT NULL DEFAULT 5368709120, -- 5 GB default
    storage_used_bytes   BIGINT NOT NULL DEFAULT 0,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT chk_users_storage_used_non_negative CHECK (storage_used_bytes >= 0)
);

-- ============================================================
-- folders (self-referencing for nested structure — FR-3.6)
-- ============================================================
CREATE TABLE folders (
    id               UUID PRIMARY KEY,
    owner_id         UUID NOT NULL REFERENCES users (id),
    parent_folder_id UUID REFERENCES folders (id),
    name             VARCHAR(255) NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_folders_owner_id ON folders (owner_id);
CREATE INDEX idx_folders_parent_folder_id ON folders (parent_folder_id);

-- ============================================================
-- storage_nodes (FR-5.4/5.5 — config-driven node registry)
-- ============================================================
CREATE TABLE storage_nodes (
    id            UUID PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    endpoint      VARCHAR(255) NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    capacity_bytes BIGINT NOT NULL,
    used_bytes    BIGINT NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_storage_nodes_name UNIQUE (name),
    CONSTRAINT chk_storage_nodes_status CHECK (status IN ('ACTIVE', 'DEGRADED', 'DOWN'))
);

-- ============================================================
-- documents (metadata only — FR-5.1; file bytes never live here)
-- current_version_id FK added after document_versions exists, below,
-- since the two tables reference each other.
-- ============================================================
CREATE TABLE documents (
    id                  UUID PRIMARY KEY,
    owner_id            UUID NOT NULL REFERENCES users (id),
    filename            VARCHAR(255) NOT NULL,
    folder_id           UUID REFERENCES folders (id),
    size_bytes          BIGINT NOT NULL,
    mime_type           VARCHAR(127) NOT NULL,
    sha256_hash         VARCHAR(64) NOT NULL,
    current_version_id UUID, -- FK added below (circular reference)
    is_deleted          BOOLEAN NOT NULL DEFAULT false,
    replication_status  VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    -- Full-text search per FR-10.1 (filename only for now; tags/content
    -- search are later phases per the PRD's own Search section).
    search_vector       TSVECTOR GENERATED ALWAYS AS (to_tsvector('english', filename)) STORED,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_documents_replication_status
        CHECK (replication_status IN ('PENDING', 'REPLICATED', 'FAILED'))
);

CREATE INDEX idx_documents_owner_id ON documents (owner_id);
CREATE INDEX idx_documents_folder_id ON documents (folder_id);
CREATE INDEX idx_documents_sha256_hash ON documents (sha256_hash); -- dedup lookups, FR-9.2
CREATE INDEX idx_documents_search_vector ON documents USING GIN (search_vector);

-- ============================================================
-- document_versions (FR-8.1 — every edit is a new version, never a mutation)
-- ============================================================
CREATE TABLE document_versions (
    id            UUID PRIMARY KEY,
    document_id   UUID NOT NULL REFERENCES documents (id),
    version_number INTEGER NOT NULL,
    size_bytes    BIGINT NOT NULL,
    sha256_hash   VARCHAR(64) NOT NULL,
    created_by    UUID NOT NULL REFERENCES users (id),
    -- Chunk list or single-object ref; kept flexible as JSONB rather than a
    -- rigid column set, since chunked vs. non-chunked storage (FR-7.5)
    -- shapes this differently.
    storage_ref   JSONB NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_document_versions_document_version UNIQUE (document_id, version_number)
);

CREATE INDEX idx_document_versions_document_id ON document_versions (document_id);

-- Now that document_versions exists, complete the circular FK.
ALTER TABLE documents
    ADD CONSTRAINT fk_documents_current_version
    FOREIGN KEY (current_version_id) REFERENCES document_versions (id);

-- ============================================================
-- chunks (FR-7.1-7.4 — large files split across nodes)
-- ============================================================
CREATE TABLE chunks (
    id                     UUID PRIMARY KEY,
    document_version_id   UUID NOT NULL REFERENCES document_versions (id),
    chunk_number           INTEGER NOT NULL,
    storage_node_id        UUID NOT NULL REFERENCES storage_nodes (id),
    checksum               VARCHAR(64) NOT NULL,
    size_bytes             BIGINT NOT NULL,

    CONSTRAINT uq_chunks_version_number UNIQUE (document_version_id, chunk_number)
);

CREATE INDEX idx_chunks_document_version_id ON chunks (document_version_id);

-- ============================================================
-- permissions (FR-4.1/4.2 — role-based sharing)
-- ============================================================
CREATE TABLE permissions (
    id          UUID PRIMARY KEY,
    document_id UUID NOT NULL REFERENCES documents (id),
    user_id     UUID NOT NULL REFERENCES users (id),
    role        VARCHAR(20) NOT NULL,
    granted_by  UUID NOT NULL REFERENCES users (id),
    granted_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_permissions_document_user UNIQUE (document_id, user_id),
    CONSTRAINT chk_permissions_role CHECK (role IN ('VIEWER', 'COMMENTER', 'EDITOR', 'OWNER'))
);

CREATE INDEX idx_permissions_document_id ON permissions (document_id);
CREATE INDEX idx_permissions_user_id ON permissions (user_id);

-- ============================================================
-- share_links (FR-4.4-4.7 — public link sharing)
-- ============================================================
CREATE TABLE share_links (
    id             UUID PRIMARY KEY,
    document_id    UUID NOT NULL REFERENCES documents (id),
    token          VARCHAR(255) NOT NULL,
    expires_at     TIMESTAMPTZ,
    password_hash  VARCHAR(255),
    download_limit INTEGER,
    downloads_used INTEGER NOT NULL DEFAULT 0,
    read_only      BOOLEAN NOT NULL DEFAULT true,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_share_links_token UNIQUE (token)
);

CREATE INDEX idx_share_links_document_id ON share_links (document_id);

-- ============================================================
-- audit_log (FR-13.1-13.4 — append-only, no updated_at by design)
-- No JPA entity yet — added in the Events/Notifications/Audit phase.
-- ============================================================
CREATE TABLE audit_log (
    id            UUID PRIMARY KEY,
    actor_id      UUID NOT NULL REFERENCES users (id),
    action        VARCHAR(100) NOT NULL,
    target_type   VARCHAR(100) NOT NULL,
    target_id     UUID NOT NULL,
    occurred_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    result        VARCHAR(20) NOT NULL,
    metadata_json JSONB,

    CONSTRAINT chk_audit_log_result CHECK (result IN ('SUCCESS', 'FAILURE'))
);

CREATE INDEX idx_audit_log_actor_id ON audit_log (actor_id);
CREATE INDEX idx_audit_log_target ON audit_log (target_type, target_id);

-- ============================================================
-- notifications (FR-12.1-12.6)
-- No JPA entity yet — added in the Events/Notifications/Audit phase.
-- ============================================================
CREATE TABLE notifications (
    id           UUID PRIMARY KEY,
    user_id      UUID NOT NULL REFERENCES users (id),
    type         VARCHAR(50) NOT NULL,
    payload_json JSONB NOT NULL,
    is_read      BOOLEAN NOT NULL DEFAULT false,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_notifications_user_id ON notifications (user_id);
CREATE INDEX idx_notifications_user_unread ON notifications (user_id) WHERE is_read = false;
