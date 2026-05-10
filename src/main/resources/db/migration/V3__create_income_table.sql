CREATE TABLE income (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT                   NOT NULL REFERENCES tenant (id),
    amount      NUMERIC(12, 2)           NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_income_tenant ON income (tenant_id);
