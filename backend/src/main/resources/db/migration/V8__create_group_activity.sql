CREATE TABLE group_activity (
                                id BIGSERIAL PRIMARY KEY,

                                group_id BIGINT NOT NULL,
                                actor_user_id BIGINT NOT NULL,

                                activity_type VARCHAR(50) NOT NULL,

                                description VARCHAR(500) NOT NULL,

                                reference_id BIGINT,

                                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                CONSTRAINT fk_group_activity_group
                                    FOREIGN KEY (group_id)
                                        REFERENCES expense_groups(id),

                                CONSTRAINT fk_group_activity_actor
                                    FOREIGN KEY (actor_user_id)
                                        REFERENCES users(id)
);

CREATE INDEX idx_group_activity_group_id
    ON group_activity(group_id);

CREATE INDEX idx_group_activity_actor_user_id
    ON group_activity(actor_user_id);

CREATE INDEX idx_group_activity_created_at
    ON group_activity(created_at);

CREATE INDEX idx_group_activity_group_created_at
    ON group_activity(group_id, created_at);