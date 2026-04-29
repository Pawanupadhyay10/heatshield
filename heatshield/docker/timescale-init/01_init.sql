-- ============================================================
-- HeatShield TimescaleDB Initialisation
-- Purpose: Time-series storage for heat readings (temp, HI)
-- ============================================================

CREATE EXTENSION IF NOT EXISTS timescaledb CASCADE;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ── Heat Readings (main time-series table) ───────────────────
-- Every weather poll from MS-1 writes a row here via Kafka → MS-2
CREATE TABLE IF NOT EXISTS heat_readings (
    time            TIMESTAMPTZ     NOT NULL,
    city_id         UUID            NOT NULL,
    city_name       TEXT            NOT NULL,
    country_code    CHAR(2)         NOT NULL,
    continent_code  CHAR(2)         NOT NULL,
    lat             DOUBLE PRECISION NOT NULL,
    lng             DOUBLE PRECISION NOT NULL,
    temp_c          FLOAT           NOT NULL,  -- dry-bulb temperature °C
    feels_like_c    FLOAT,
    humidity        FLOAT           NOT NULL,  -- relative humidity %
    wind_speed_ms   FLOAT,
    uv_index        FLOAT,
    -- Heat Index computed by MS-2 using Steadman formula
    heat_index      FLOAT           NOT NULL,
    -- 5-tier WHO-aligned classification
    risk_tier       VARCHAR(10)     NOT NULL
                        CHECK (risk_tier IN ('SAFE','WATCH','WARNING','DANGER','EXTREME')),
    -- Trend compared to previous reading for this city
    trend           VARCHAR(8)
                        CHECK (trend IN ('RISING','STABLE','FALLING')),
    data_source     VARCHAR(20) DEFAULT 'openweathermap'
);

-- Convert to TimescaleDB hypertable — partitioned by time
-- chunk_time_interval: each chunk = 1 day of data
SELECT create_hypertable(
    'heat_readings',
    'time',
    chunk_time_interval => INTERVAL '1 day',
    if_not_exists => TRUE
);

-- Composite index: city + time (most common query pattern)
CREATE INDEX IF NOT EXISTS idx_heat_readings_city_time
    ON heat_readings(city_id, time DESC);

-- Index for continent-level queries (agent scans by region)
CREATE INDEX IF NOT EXISTS idx_heat_readings_continent_time
    ON heat_readings(continent_code, time DESC);

-- Index for risk tier filtering (fetch all DANGER+ cities)
CREATE INDEX IF NOT EXISTS idx_heat_readings_risk_time
    ON heat_readings(risk_tier, time DESC);

-- ── Compression policy ───────────────────────────────────────
-- Compress chunks older than 7 days (keeps storage lean)
ALTER TABLE heat_readings SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'city_id, continent_code'
);

SELECT add_compression_policy('heat_readings',
    compress_after => INTERVAL '7 days',
    if_not_exists => TRUE
);

-- ── Retention policy ─────────────────────────────────────────
-- Auto-drop data older than 2 years (configurable)
SELECT add_retention_policy('heat_readings',
    drop_after => INTERVAL '2 years',
    if_not_exists => TRUE
);

-- ── Continuous aggregate: hourly city averages ───────────────
-- Pre-computed rollup — used by agent for trend analysis
-- Much faster than GROUP BY on raw table at scale
CREATE MATERIALIZED VIEW IF NOT EXISTS heat_readings_hourly
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 hour', time) AS bucket,
    city_id,
    city_name,
    country_code,
    continent_code,
    AVG(heat_index)     AS avg_heat_index,
    MAX(heat_index)     AS max_heat_index,
    MIN(heat_index)     AS min_heat_index,
    AVG(temp_c)         AS avg_temp_c,
    AVG(humidity)       AS avg_humidity,
    -- Most frequent risk tier in the hour
    MODE() WITHIN GROUP (ORDER BY risk_tier) AS dominant_risk_tier,
    COUNT(*)            AS reading_count
FROM heat_readings
GROUP BY bucket, city_id, city_name, country_code, continent_code
WITH NO DATA;

-- Refresh policy: keep hourly aggregate up-to-date
SELECT add_continuous_aggregate_policy('heat_readings_hourly',
    start_offset => INTERVAL '3 hours',
    end_offset   => INTERVAL '1 hour',
    schedule_interval => INTERVAL '1 hour',
    if_not_exists => TRUE
);
