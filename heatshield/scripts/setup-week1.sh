#!/usr/bin/env bash
# =============================================================
# HeatShield — Week 1 Complete Setup Script
# Run this from the root of the heatshield/ directory
# =============================================================
set -e  # exit on any error

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

log()  { echo -e "${GREEN}[SETUP]${NC} $1"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
info() { echo -e "${BLUE}[INFO]${NC} $1"; }
err()  { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }

# ── Step 0: Prerequisites check ──────────────────────────────
log "Checking prerequisites..."

command -v java   >/dev/null 2>&1 || err "Java 21+ required. Install: sdk install java 21-tem"
command -v mvn    >/dev/null 2>&1 || err "Maven required. Install: brew install maven / sdk install maven"
command -v docker >/dev/null 2>&1 || err "Docker required. Install Docker Desktop."

JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    err "Java 21+ required. Current: $JAVA_VERSION"
fi
log "Java $JAVA_VERSION ✓"

# ── Step 1: Environment file ──────────────────────────────────
log "Creating .env file..."
if [ ! -f .env ]; then
    cat > .env << 'EOF'
# =============================================================
# HeatShield Environment Variables
# Fill in real values before running services
# =============================================================

# OpenWeatherMap — free tier at https://openweathermap.org/api
# 1M API calls/month free — more than enough for development
OWM_API_KEY=your_openweathermap_api_key_here

# OpenAI — for LangChain4j agent (GPT-4o)
# OR swap for Anthropic/Gemini in agent-service config
OPENAI_API_KEY=your_openai_api_key_here

# Database passwords (already set in docker-compose.yml)
POSTGIS_PASSWORD=heatshield_dev_password
TIMESCALE_PASSWORD=heatshield_dev_password

# Twilio (optional for Week 1 — needed in Week 6)
TWILIO_ACCOUNT_SID=your_twilio_sid
TWILIO_AUTH_TOKEN=your_twilio_token
TWILIO_FROM_NUMBER=+1234567890

# Mapbox (for React frontend — Week 7)
MAPBOX_ACCESS_TOKEN=your_mapbox_token
EOF
    warn ".env created — FILL IN your OWM_API_KEY and OPENAI_API_KEY before proceeding!"
else
    log ".env already exists ✓"
fi

# Source the env file
export $(grep -v '^#' .env | xargs)

# ── Step 2: Start infrastructure ──────────────────────────────
log "Starting Docker infrastructure (Kafka, PostGIS, TimescaleDB, Redis)..."
docker compose up -d

log "Waiting for services to be healthy..."
sleep 10

# Wait for Kafka
MAX_TRIES=30
TRIES=0
until docker exec hs-kafka kafka-topics --bootstrap-server localhost:9092 --list >/dev/null 2>&1; do
    TRIES=$((TRIES+1))
    if [ $TRIES -ge $MAX_TRIES ]; then
        err "Kafka failed to start after ${MAX_TRIES} attempts"
    fi
    info "Waiting for Kafka... ($TRIES/$MAX_TRIES)"
    sleep 5
done
log "Kafka is ready ✓"

# Wait for PostGIS
until docker exec hs-postgis pg_isready -U heatshield >/dev/null 2>&1; do
    info "Waiting for PostGIS..."
    sleep 3
done
log "PostGIS is ready ✓"

# Wait for TimescaleDB
until docker exec hs-timescaledb pg_isready -U heatshield >/dev/null 2>&1; do
    info "Waiting for TimescaleDB..."
    sleep 3
done
log "TimescaleDB is ready ✓"

# Wait for Redis
until docker exec hs-redis redis-cli ping | grep -q PONG; do
    info "Waiting for Redis..."
    sleep 2
done
log "Redis is ready ✓"

# ── Step 3: Verify Kafka topics were auto-created ──────────────
log "Verifying Kafka topics..."
TOPICS=$(docker exec hs-kafka kafka-topics --bootstrap-server localhost:9092 --list)
for topic in heat-events risk-alerts alert-commands agent-dlq; do
    if echo "$TOPICS" | grep -q "^$topic$"; then
        info "  ✓ $topic"
    else
        warn "  ✗ $topic missing — creating..."
        docker exec hs-kafka kafka-topics \
            --bootstrap-server localhost:9092 \
            --create --topic $topic \
            --partitions 6 --replication-factor 1
    fi
done

# ── Step 4: Verify PostGIS schema ──────────────────────────────
log "Verifying PostGIS tables..."
TABLES=$(docker exec hs-postgis psql -U heatshield -d heatshield_spatial -t \
    -c "SELECT tablename FROM pg_tables WHERE schemaname='public';")
for table in vulnerability_zones cool_shelters cities agent_runs; do
    if echo "$TABLES" | grep -q "$table"; then
        info "  ✓ $table"
    else
        err "Table $table missing — check docker/postgis-init/01_init.sql"
    fi
done

# ── Step 5: Verify TimescaleDB hypertable ──────────────────────
log "Verifying TimescaleDB hypertable..."
HYPER=$(docker exec hs-timescaledb psql -U heatshield -d heatshield_timeseries -t \
    -c "SELECT hypertable_name FROM timescaledb_information.hypertables;")
if echo "$HYPER" | grep -q "heat_readings"; then
    info "  ✓ heat_readings hypertable"
else
    err "heat_readings hypertable not created — check docker/timescale-init/01_init.sql"
fi

# ── Step 6: Verify seed cities ──────────────────────────────────
log "Checking seed cities..."
CITY_COUNT=$(docker exec hs-postgis psql -U heatshield -d heatshield_spatial -t \
    -c "SELECT COUNT(*) FROM cities;" | tr -d ' \n')
info "  ${CITY_COUNT} pilot cities loaded"

# ── Step 7: Build Java services ───────────────────────────────
log "Building Spring Boot services (skip tests for Week 1 speed)..."
mvn clean install -DskipTests -q
log "Build complete ✓"

# ── Step 8: Health check summary ──────────────────────────────
echo ""
echo "============================================================"
echo -e "${GREEN}HeatShield Week 1 Setup Complete!${NC}"
echo "============================================================"
echo ""
echo "📊 Infrastructure URLs:"
echo "  Kafka UI      → http://localhost:8090"
echo "  pgAdmin       → http://localhost:5050  (dev@heatshield.local / admin)"
echo "  PostGIS       → localhost:5433  (heatshield / heatshield_dev_password)"
echo "  TimescaleDB   → localhost:5434  (heatshield / heatshield_dev_password)"
echo "  Redis         → localhost:6379"
echo ""
echo "🚀 Next steps:"
echo "  1. Add your OWM_API_KEY to .env"
echo "  2. cd weather-ingestion-service && mvn spring-boot:run"
echo "  3. Open Kafka UI → topic heat-events → watch messages flow!"
echo ""
echo "📖 Week 1 goal: see HeatEvent messages appear in Kafka UI"
echo "   for Haridwar, Delhi, Mumbai, Cairo, Madrid"
echo "============================================================"
