CREATE TABLE recurring_expenses (
                                    id BIGSERIAL PRIMARY KEY,
                                    user_id BIGINT NOT NULL,
                                    title VARCHAR(120) NOT NULL,
                                    amount NUMERIC(12, 2) NOT NULL,
                                    category VARCHAR(50) NOT NULL,
                                    frequency VARCHAR(30) NOT NULL,
                                    next_due_date DATE NOT NULL,
                                    active BOOLEAN NOT NULL DEFAULT TRUE,
                                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                    CONSTRAINT fk_recurring_expenses_user
                                        FOREIGN KEY (user_id)
                                            REFERENCES users(id)
                                            ON DELETE CASCADE,

                                    CONSTRAINT chk_recurring_expense_amount
                                        CHECK (amount > 0)
);

CREATE INDEX idx_recurring_expenses_user
    ON recurring_expenses(user_id);

CREATE INDEX idx_recurring_expenses_user_due_date
    ON recurring_expenses(user_id, next_due_date);

CREATE INDEX idx_recurring_expenses_user_active_due
    ON recurring_expenses(user_id, active, next_due_date);