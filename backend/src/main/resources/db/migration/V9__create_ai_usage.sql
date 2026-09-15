CREATE TABLE ai_usage (
                          id BIGSERIAL PRIMARY KEY,
                          user_id BIGINT NOT NULL,
                          usage_date DATE NOT NULL,
                          request_count INTEGER NOT NULL DEFAULT 0,
                          created_at TIMESTAMP NOT NULL,
                          updated_at TIMESTAMP NOT NULL,

                          CONSTRAINT fk_ai_usage_user
                              FOREIGN KEY (user_id)
                                  REFERENCES users(id)
                                  ON DELETE CASCADE,

                          CONSTRAINT uk_ai_usage_user_date
                              UNIQUE (user_id, usage_date),

                          CONSTRAINT chk_ai_usage_request_count
                              CHECK (request_count >= 0)
);

CREATE INDEX idx_ai_usage_user_date
    ON ai_usage(user_id, usage_date);