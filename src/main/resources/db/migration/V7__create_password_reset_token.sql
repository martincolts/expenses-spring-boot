CREATE TABLE password_reset_token (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT                   NOT NULL REFERENCES tenant (id) ON DELETE CASCADE,
    token       VARCHAR(255)             NOT NULL,
    reset_url   VARCHAR(512)             NOT NULL,
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at     TIMESTAMP WITH TIME ZONE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_password_reset_token UNIQUE (token)
);

CREATE INDEX idx_password_reset_token_tenant ON password_reset_token (tenant_id);
