CREATE TABLE tenant (
    id             BIGSERIAL PRIMARY KEY,
    name           VARCHAR(255)             NOT NULL,
    email          VARCHAR(255)             NOT NULL,
    password_hash  VARCHAR(255)             NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_tenant_email UNIQUE (email)
);

CREATE TABLE category (
    id           BIGSERIAL PRIMARY KEY,
    tenant_id    BIGINT                   NOT NULL REFERENCES tenant (id),
    name         VARCHAR(50)              NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_category_tenant_name UNIQUE (tenant_id, name)
);

CREATE INDEX idx_category_tenant ON category (tenant_id);

CREATE TYPE expense_status AS ENUM ('PAID', 'NOT_PAUD_YET');

CREATE TABLE expense (
    id           BIGSERIAL PRIMARY KEY,
    tenant_id    BIGINT                   NOT NULL REFERENCES tenant (id),
    category_id  BIGINT                            REFERENCES category (id),
    description  VARCHAR(255)             NOT NULL,
    amount       NUMERIC(12, 2)           NOT NULL,
    currency     CHAR(3)                  NOT NULL,
    status       expense_status           NOT NULL DEFAULT 'NOT_PAUD_YET',
    incurred_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    paid_at      TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_expense_incurred_at  ON expense (incurred_at);
CREATE INDEX idx_expense_tenant       ON expense (tenant_id);
CREATE INDEX idx_expense_category_id  ON expense (category_id);
