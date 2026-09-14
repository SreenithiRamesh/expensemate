CREATE TABLE settlements (
                             id BIGSERIAL PRIMARY KEY,

                             group_id BIGINT NOT NULL,

                             from_user_id BIGINT NOT NULL,

                             to_user_id BIGINT NOT NULL,

                             amount NUMERIC(19, 2) NOT NULL,

                             settlement_mode VARCHAR(20) NOT NULL,

                             idempotency_key VARCHAR(100) NOT NULL,

                             created_by BIGINT NOT NULL,

                             settled_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                             created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                             CONSTRAINT fk_settlement_group
                                 FOREIGN KEY (group_id)
                                     REFERENCES expense_groups(id),

                             CONSTRAINT fk_settlement_from_user
                                 FOREIGN KEY (from_user_id)
                                     REFERENCES users(id),

                             CONSTRAINT fk_settlement_to_user
                                 FOREIGN KEY (to_user_id)
                                     REFERENCES users(id),

                             CONSTRAINT fk_settlement_created_by
                                 FOREIGN KEY (created_by)
                                     REFERENCES users(id),

                             CONSTRAINT chk_settlement_positive_amount
                                 CHECK (amount > 0),

                             CONSTRAINT chk_settlement_different_users
                                 CHECK (from_user_id <> to_user_id),

                             CONSTRAINT uk_settlement_idempotency_key
                                 UNIQUE (idempotency_key)
);

CREATE INDEX idx_settlements_group_id
    ON settlements(group_id);

CREATE INDEX idx_settlements_from_user_id
    ON settlements(from_user_id);

CREATE INDEX idx_settlements_to_user_id
    ON settlements(to_user_id);

CREATE INDEX idx_settlements_settled_at
    ON settlements(settled_at);