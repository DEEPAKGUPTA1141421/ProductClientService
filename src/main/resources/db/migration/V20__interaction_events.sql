-- =============================================================================
-- V20: user_interaction_events partitioned table
-- =============================================================================
-- Run manually in each environment BEFORE first app boot. Hibernate's
-- ddl-auto=update cannot create a partitioned parent table.
--
-- Partitioning strategy: RANGE on event_ts, one partition per calendar month.
-- Rationale:
--   - Oldest partitions (>6 months) can be detached and archived to S3.
--   - BRIN index on event_ts works well on append-only monthly partitions.
--   - Query planner prunes partitions on typical "last N days" CF windows.
--
-- Bootstrap creates 3 months. Use pg_partman (or a cron) to maintain a
-- rolling window of future partitions.
-- =============================================================================

CREATE TABLE IF NOT EXISTS user_interaction_events (
    id         BIGSERIAL,
    user_id    UUID,
    product_id UUID        NOT NULL,
    event_type SMALLINT    NOT NULL,
    session_id UUID,
    dwell_ms   INTEGER,
    event_ts   TIMESTAMPTZ NOT NULL,
    source     VARCHAR(16),
    PRIMARY KEY (id, event_ts)
) PARTITION BY RANGE (event_ts);

-- Bootstrap partitions (adjust quarter on first run).
CREATE TABLE IF NOT EXISTS user_interaction_events_2026_04
    PARTITION OF user_interaction_events
    FOR VALUES FROM ('2026-04-01') TO ('2026-05-01');

CREATE TABLE IF NOT EXISTS user_interaction_events_2026_05
    PARTITION OF user_interaction_events
    FOR VALUES FROM ('2026-05-01') TO ('2026-06-01');

CREATE TABLE IF NOT EXISTS user_interaction_events_2026_06
    PARTITION OF user_interaction_events
    FOR VALUES FROM ('2026-06-01') TO ('2026-07-01');

CREATE INDEX IF NOT EXISTS idx_uie_user_ts
    ON user_interaction_events (user_id, event_ts DESC);

CREATE INDEX IF NOT EXISTS idx_uie_product_ts
    ON user_interaction_events (product_id, event_ts DESC);

CREATE INDEX IF NOT EXISTS idx_uie_session
    ON user_interaction_events (session_id);

CREATE INDEX IF NOT EXISTS idx_uie_event_ts_brin
    ON user_interaction_events USING BRIN (event_ts);
