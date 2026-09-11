CREATE TABLE personal_expenses (
                                   id BIGSERIAL PRIMARY KEY,

                                   user_id BIGINT NOT NULL,

                                   amount NUMERIC(12, 2) NOT NULL,

                                   category VARCHAR(50) NOT NULL,

                                   expense_date DATE NOT NULL,

                                   description VARCHAR(255),

                                   created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                   updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                   CONSTRAINT fk_personal_expenses_user
                                       FOREIGN KEY (user_id)
                                           REFERENCES users(id)
                                           ON DELETE CASCADE,

                                   CONSTRAINT chk_personal_expense_amount
                                       CHECK (amount > 0)
);

CREATE INDEX idx_personal_expenses_user_id
    ON personal_expenses(user_id);

CREATE INDEX idx_personal_expenses_user_date
    ON personal_expenses(user_id, expense_date);

CREATE INDEX idx_personal_expenses_user_category
    ON personal_expenses(user_id, category);