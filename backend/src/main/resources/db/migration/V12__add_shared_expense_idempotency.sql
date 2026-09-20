ALTER TABLE shared_expenses
    ADD COLUMN created_by BIGINT,
    ADD COLUMN idempotency_key VARCHAR(100),
    ADD COLUMN request_fingerprint VARCHAR(64);

ALTER TABLE shared_expenses
    ADD CONSTRAINT fk_shared_expenses_created_by
        FOREIGN KEY (created_by)
            REFERENCES users(id);

ALTER TABLE shared_expenses
    ADD CONSTRAINT uk_shared_expense_user_idempotency
        UNIQUE (created_by, idempotency_key);

CREATE INDEX idx_shared_expenses_created_by
    ON shared_expenses(created_by);