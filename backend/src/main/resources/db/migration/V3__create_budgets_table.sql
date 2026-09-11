CREATE TABLE budgets (
                         id BIGSERIAL PRIMARY KEY,
                         user_id BIGINT NOT NULL,
                         category VARCHAR(50) NOT NULL,
                         monthly_limit NUMERIC(12, 2) NOT NULL,
                         month INTEGER NOT NULL,
                         year INTEGER NOT NULL,
                         created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                         CONSTRAINT fk_budgets_user
                             FOREIGN KEY (user_id)
                                 REFERENCES users(id)
                                 ON DELETE CASCADE,

                         CONSTRAINT chk_budget_monthly_limit
                             CHECK (monthly_limit > 0),

                         CONSTRAINT chk_budget_month
                             CHECK (month BETWEEN 1 AND 12),

    CONSTRAINT chk_budget_year
        CHECK (year >= 2000),

    CONSTRAINT uq_budget_user_category_month_year
        UNIQUE (user_id, category, month, year)
);

CREATE INDEX idx_budgets_user_id
    ON budgets(user_id);

CREATE INDEX idx_budgets_user_period
    ON budgets(user_id, year, month);