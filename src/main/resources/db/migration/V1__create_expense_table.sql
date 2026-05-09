CREATE TABLE expense (
    id           BIGSERIAL PRIMARY KEY,
    description  VARCHAR(255)             NOT NULL,
    amount       NUMERIC(12, 2)           NOT NULL,
    currency     CHAR(3)                  NOT NULL,
    category     VARCHAR(50),
    incurred_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_expense_incurred_at ON expense (incurred_at);
CREATE INDEX idx_expense_category    ON expense (category);
