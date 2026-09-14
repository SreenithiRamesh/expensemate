CREATE TABLE shared_expenses (
                                 id BIGSERIAL PRIMARY KEY,

                                 group_id BIGINT NOT NULL,
                                 paid_by BIGINT NOT NULL,

                                 title VARCHAR(150) NOT NULL,
                                 amount NUMERIC(12, 2) NOT NULL,
                                 split_type VARCHAR(30) NOT NULL,
                                 expense_date DATE NOT NULL,

                                 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                 CONSTRAINT fk_shared_expenses_group
                                     FOREIGN KEY (group_id)
                                         REFERENCES expense_groups(id)
                                         ON DELETE CASCADE,

                                 CONSTRAINT fk_shared_expenses_paid_by
                                     FOREIGN KEY (paid_by)
                                         REFERENCES users(id),

                                 CONSTRAINT chk_shared_expense_amount
                                     CHECK (amount > 0)
);

CREATE TABLE expense_splits (
                                id BIGSERIAL PRIMARY KEY,

                                expense_id BIGINT NOT NULL,
                                user_id BIGINT NOT NULL,

                                share_amount NUMERIC(12, 2) NOT NULL,
                                percentage NUMERIC(7, 4),

                                CONSTRAINT fk_expense_splits_expense
                                    FOREIGN KEY (expense_id)
                                        REFERENCES shared_expenses(id)
                                        ON DELETE CASCADE,

                                CONSTRAINT fk_expense_splits_user
                                    FOREIGN KEY (user_id)
                                        REFERENCES users(id),

                                CONSTRAINT chk_expense_split_share_amount
                                    CHECK (share_amount >= 0),

                                CONSTRAINT chk_expense_split_percentage
                                    CHECK (
                                        percentage IS NULL
                                            OR (percentage >= 0 AND percentage <= 100)
                                        ),

                                CONSTRAINT uq_expense_split_expense_user
                                    UNIQUE (expense_id, user_id)
);

CREATE INDEX idx_shared_expenses_group_id
    ON shared_expenses(group_id);

CREATE INDEX idx_shared_expenses_paid_by
    ON shared_expenses(paid_by);

CREATE INDEX idx_shared_expenses_group_date
    ON shared_expenses(group_id, expense_date);

CREATE INDEX idx_expense_splits_expense_id
    ON expense_splits(expense_id);

CREATE INDEX idx_expense_splits_user_id
    ON expense_splits(user_id);