CREATE TABLE monthly_insight_cache (
                                       id BIGSERIAL PRIMARY KEY,

                                       user_id BIGINT NOT NULL,

                                       insight_month VARCHAR(7) NOT NULL,

                                       insight TEXT NOT NULL,

                                       created_at TIMESTAMP NOT NULL,

                                       CONSTRAINT fk_monthly_insight_cache_user
                                           FOREIGN KEY (user_id)
                                               REFERENCES users(id)
                                               ON DELETE CASCADE,

                                       CONSTRAINT uk_monthly_insight_user_month
                                           UNIQUE (user_id, insight_month)
);

CREATE INDEX idx_monthly_insight_user_month
    ON monthly_insight_cache(user_id, insight_month);