-- ============================================================
-- HeatShield PostGIS Initialisation
-- Runs automatically when container first starts
-- ============================================================

-- Enable PostGIS extension (spatial data types + functions)
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS postgis_topology;
-- UUID generation
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
-- pg_trgm for text search on city/zone names
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- ── Vulnerability Zones ──────────────────────────────────────
-- Stores population density + demographic data per grid cell
-- Data source: WorldPop 1km raster → loaded as polygon grid
CREATE TABLE IF NOT EXISTS vulnerability_zones (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    zone_name           TEXT NOT NULL,
    country_code        CHAR(2) NOT NULL,
    continent_code      CHAR(2) NOT NULL,  -- AF, AS, EU, NA, SA, OC
    -- EPSG:4326 = global WGS84 coordinate system
    geom                GEOMETRY(POLYGON, 4326) NOT NULL,
    pop_density         FLOAT NOT NULL DEFAULT 0,   -- persons/km²
    pct_elderly         FLOAT NOT NULL DEFAULT 0,   -- % age 65+
    pct_children        FLOAT NOT NULL DEFAULT 0,   -- % age under 5
    -- Computed vulnerability score (stored for query performance)
    -- Formula: weighted sum of density + age vulnerability
    vulnerability_score FLOAT GENERATED ALWAYS AS (
        LEAST(100.0,
            (pop_density / 10000.0 * 40.0) +  -- density component (40%)
            (pct_elderly * 40.0) +              -- elderly component  (40%)
            (pct_children * 20.0)               -- children component (20%)
        )
    ) STORED,
    created_at          TIMESTAMPTZ DEFAULT NOW(),
    updated_at          TIMESTAMPTZ DEFAULT NOW()
);

-- GiST spatial index — enables fast ST_Within, ST_DWithin queries
CREATE INDEX IF NOT EXISTS idx_vulnerability_zones_geom
    ON vulnerability_zones USING GIST(geom);

-- B-tree indexes for filtering by country/continent
CREATE INDEX IF NOT EXISTS idx_vulnerability_zones_country
    ON vulnerability_zones(country_code);
CREATE INDEX IF NOT EXISTS idx_vulnerability_zones_continent
    ON vulnerability_zones(continent_code);

-- ── Cool Shelters ────────────────────────────────────────────
-- Populated from OSM Overpass API: hospitals, malls, metros
CREATE TABLE IF NOT EXISTS cool_shelters (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    osm_id          BIGINT UNIQUE,
    name            TEXT NOT NULL,
    shelter_type    VARCHAR(30) NOT NULL
                        CHECK (shelter_type IN (
                            'hospital', 'mall', 'metro_station',
                            'library', 'community_center', 'hotel', 'airport'
                        )),
    location        GEOMETRY(POINT, 4326) NOT NULL,
    address         TEXT,
    country_code    CHAR(2),
    capacity_est    INT DEFAULT 0,
    ac_confirmed    BOOLEAN DEFAULT FALSE,   -- air conditioning confirmed
    is_verified     BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_cool_shelters_location
    ON cool_shelters USING GIST(location);

CREATE INDEX IF NOT EXISTS idx_cool_shelters_country
    ON cool_shelters(country_code);

-- ── Cities Reference Table ───────────────────────────────────
-- Master list of cities to poll for weather data
CREATE TABLE IF NOT EXISTS cities (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    owm_city_id     BIGINT UNIQUE,          -- OpenWeatherMap city ID
    name            TEXT NOT NULL,
    country_code    CHAR(2) NOT NULL,
    continent_code  CHAR(2) NOT NULL,
    location        GEOMETRY(POINT, 4326) NOT NULL,
    timezone        TEXT,
    population      BIGINT DEFAULT 0,
    is_active       BOOLEAN DEFAULT TRUE,   -- toggle polling on/off per city
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_cities_location
    ON cities USING GIST(location);
CREATE INDEX IF NOT EXISTS idx_cities_country
    ON cities(country_code);

-- ── Agent Run Audit Log ──────────────────────────────────────
-- Every agent execution logged here — critical for production
CREATE TABLE IF NOT EXISTS agent_runs (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    triggered_by        VARCHAR(20) NOT NULL
                            CHECK (triggered_by IN ('schedule', 'risk_event', 'manual')),
    zone_id             UUID REFERENCES vulnerability_zones(id),
    city_id             UUID REFERENCES cities(id),
    heat_index_at_run   FLOAT,
    vulnerability_score FLOAT,
    tools_called        JSONB NOT NULL DEFAULT '[]',
    -- Each entry: {tool, input, output, duration_ms, success}
    alert_dispatched    BOOLEAN DEFAULT FALSE,
    alert_severity      VARCHAR(10),
    fallback_used       BOOLEAN DEFAULT FALSE,  -- TRUE if LLM was down
    llm_tokens_used     INT DEFAULT 0,
    total_duration_ms   INT,
    error_message       TEXT,
    created_at          TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_agent_runs_created
    ON agent_runs(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_agent_runs_zone
    ON agent_runs(zone_id);

-- ── Notification Preferences ─────────────────────────────────
CREATE TABLE IF NOT EXISTS user_alert_preferences (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id             UUID NOT NULL UNIQUE,
    email               TEXT,
    phone               TEXT,
    fcm_token           TEXT,
    min_risk_tier       VARCHAR(10) DEFAULT 'WARNING',
    -- Only alert if zone risk >= this tier
    quiet_hours_start   TIME DEFAULT '22:00',
    quiet_hours_end     TIME DEFAULT '07:00',
    max_alerts_per_hour INT DEFAULT 1,
    created_at          TIMESTAMPTZ DEFAULT NOW(),
    updated_at          TIMESTAMPTZ DEFAULT NOW()
);

-- ── Seed: 10 pilot cities for Week 1 development ─────────────
INSERT INTO cities (owm_city_id, name, country_code, continent_code, location, timezone, population)
VALUES
    (1275339, 'Mumbai',     'IN', 'AS', ST_SetSRID(ST_MakePoint(72.8777, 18.9667), 4326), 'Asia/Kolkata',    20667656),
    (1261481, 'New Delhi',  'IN', 'AS', ST_SetSRID(ST_MakePoint(77.2090, 28.6139), 4326), 'Asia/Kolkata',    32941000),
    (1277333, 'Bengaluru',  'IN', 'AS', ST_SetSRID(ST_MakePoint(77.5946, 12.9716), 4326), 'Asia/Kolkata',    12765000),
    (1264527, 'Chennai',    'IN', 'AS', ST_SetSRID(ST_MakePoint(80.2707, 13.0827), 4326), 'Asia/Kolkata',    10971108),
    -- Haridwar — your home city, use for live demo!
    (1270689, 'Haridwar',   'IN', 'AS', ST_SetSRID(ST_MakePoint(78.1642, 29.9457), 4326), 'Asia/Kolkata',     228832),
    (360630,  'Cairo',      'EG', 'AF', ST_SetSRID(ST_MakePoint(31.2357, 30.0444), 4326), 'Africa/Cairo',    21750000),
    (3117735, 'Madrid',     'ES', 'EU', ST_SetSRID(ST_MakePoint(-3.7038, 40.4168), 4326), 'Europe/Madrid',    3223000),
    (5368361, 'Los Angeles','US', 'NA', ST_SetSRID(ST_MakePoint(-118.2437, 34.0522),4326),'America/Los_Angeles',3898747),
    (3448439, 'São Paulo',  'BR', 'SA', ST_SetSRID(ST_MakePoint(-46.6333,-23.5505), 4326), 'America/Sao_Paulo',22430000),
    (2147714, 'Sydney',     'AU', 'OC', ST_SetSRID(ST_MakePoint(151.2093,-33.8688), 4326), 'Australia/Sydney',  5312000)
ON CONFLICT (owm_city_id) DO NOTHING;
