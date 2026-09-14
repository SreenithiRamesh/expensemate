CREATE TABLE expense_groups (
                                id BIGSERIAL PRIMARY KEY,

                                name VARCHAR(120) NOT NULL,
                                description VARCHAR(255),

                                created_by BIGINT NOT NULL,

                                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                CONSTRAINT fk_expense_groups_created_by
                                    FOREIGN KEY (created_by)
                                        REFERENCES users(id)
);

CREATE TABLE group_members (
                               id BIGSERIAL PRIMARY KEY,

                               group_id BIGINT NOT NULL,
                               user_id BIGINT NOT NULL,

                               joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                               CONSTRAINT fk_group_members_group
                                   FOREIGN KEY (group_id)
                                       REFERENCES expense_groups(id)
                                       ON DELETE CASCADE,

                               CONSTRAINT fk_group_members_user
                                   FOREIGN KEY (user_id)
                                       REFERENCES users(id),

                               CONSTRAINT uq_group_members_group_user
                                   UNIQUE (group_id, user_id)
);

CREATE INDEX idx_expense_groups_created_by
    ON expense_groups(created_by);

CREATE INDEX idx_group_members_group_id
    ON group_members(group_id);

CREATE INDEX idx_group_members_user_id
    ON group_members(user_id);