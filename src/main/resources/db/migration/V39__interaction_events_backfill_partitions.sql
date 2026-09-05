-- =============================================================================
-- V39: Backfill missing user_interaction_events partitions + catch-all default
-- =============================================================================
-- V20 only bootstrapped 2026-04..2026-06. Every insert with event_ts outside
-- an existing partition range fails with:
--   "no partition of relation "user_interaction_events" found for row"
-- which is exactly what has been happening since July 2026 (observed failing
-- at 2026-09-04). Backfill through end of 2027, and attach a DEFAULT
-- partition so an out-of-range event_ts never hard-fails inserts again — the
-- monthly partition job (InteractionPartitionMaintenanceJob) keeps moving
-- rows out of DEFAULT into proper monthly partitions going forward is not
-- required since it creates partitions ahead of time, but DEFAULT remains as
-- a safety net.
-- =============================================================================

CREATE TABLE IF NOT EXISTS user_interaction_events_2026_07
    PARTITION OF user_interaction_events
    FOR VALUES FROM ('2026-07-01') TO ('2026-08-01');

CREATE TABLE IF NOT EXISTS user_interaction_events_2026_08
    PARTITION OF user_interaction_events
    FOR VALUES FROM ('2026-08-01') TO ('2026-09-01');

CREATE TABLE IF NOT EXISTS user_interaction_events_2026_09
    PARTITION OF user_interaction_events
    FOR VALUES FROM ('2026-09-01') TO ('2026-10-01');

CREATE TABLE IF NOT EXISTS user_interaction_events_2026_10
    PARTITION OF user_interaction_events
    FOR VALUES FROM ('2026-10-01') TO ('2026-11-01');

CREATE TABLE IF NOT EXISTS user_interaction_events_2026_11
    PARTITION OF user_interaction_events
    FOR VALUES FROM ('2026-11-01') TO ('2026-12-01');

CREATE TABLE IF NOT EXISTS user_interaction_events_2026_12
    PARTITION OF user_interaction_events
    FOR VALUES FROM ('2026-12-01') TO ('2027-01-01');

CREATE TABLE IF NOT EXISTS user_interaction_events_2027_01
    PARTITION OF user_interaction_events
    FOR VALUES FROM ('2027-01-01') TO ('2027-02-01');

CREATE TABLE IF NOT EXISTS user_interaction_events_2027_02
    PARTITION OF user_interaction_events
    FOR VALUES FROM ('2027-02-01') TO ('2027-03-01');

CREATE TABLE IF NOT EXISTS user_interaction_events_2027_03
    PARTITION OF user_interaction_events
    FOR VALUES FROM ('2027-03-01') TO ('2027-04-01');

-- Catch-all safety net: anything outside every explicit monthly range lands
-- here instead of failing the insert outright.
CREATE TABLE IF NOT EXISTS user_interaction_events_default
    PARTITION OF user_interaction_events DEFAULT;
