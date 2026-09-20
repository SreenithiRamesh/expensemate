-- ============================================================
-- M20 - Authentication Hardening
-- Adds account lockout state and server-side refresh tokens.
-- Raw refresh tokens are never stored in the database.
-- ============================================================

ALTER TABLE users
    ADD COLUMN failed_login_attempts INTEGER NOT NULL DEFAULT 0;

ALTER TABLE users
    ADD COLUMN locked_until TIMESTAMP NULL;


CREATE TABLE refresh_tokens (
                                id BIGSERIAL PRIMARY KEY,

                                user_id BIGINT NOT NULL,

                                token_hash VARCHAR(64) NOT NULL,

                                expires_at TIMESTAMP NOT NULL,

                                created_at TIMESTAMP NOT NULL,

                                revoked_at TIMESTAMP NULL,

                                replaced_by_token_id BIGINT NULL,

                                CONSTRAINT fk_refresh_tokens_user
                                    FOREIGN KEY (user_id)
                                        REFERENCES users(id)
                                        ON DELETE CASCADE,

                                CONSTRAINT uk_refresh_tokens_token_hash
                                    UNIQUE (token_hash),

                                CONSTRAINT fk_refresh_tokens_replaced_by
                                    FOREIGN KEY (replaced_by_token_id)
                                        REFERENCES refresh_tokens(id)
                                        ON DELETE SET NULL
);

CREATE INDEX idx_refresh_tokens_user_id
    ON refresh_tokens(user_id);

CREATE INDEX idx_refresh_tokens_expires_at
    ON refresh_tokens(expires_at);