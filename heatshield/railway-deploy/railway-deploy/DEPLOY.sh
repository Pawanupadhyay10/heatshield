#!/usr/bin/env bash
# =============================================================
# HeatShield — Railway Deployment Guide
# =============================================================

# STEP 1 — Copy Dockerfiles to each service
# Run from: /workspaces/heatshield/heatshield/

cp railway-deploy/ms1/Dockerfile weather-ingestion-service/Dockerfile
cp railway-deploy/ms1/railway.toml weather-ingestion-service/railway.toml

cp railway-deploy/ms2/Dockerfile heat-processing-service/Dockerfile
cp railway-deploy/ms2/railway.toml heat-processing-service/railway.toml

cp railway-deploy/ms3/Dockerfile agent-service/Dockerfile
cp railway-deploy/ms3/railway.toml agent-service/railway.toml

cp railway-deploy/ms4/Dockerfile routing-service/Dockerfile
cp railway-deploy/ms4/railway.toml routing-service/railway.toml

cp railway-deploy/ms5/Dockerfile notification-service/Dockerfile
cp railway-deploy/ms5/railway.toml notification-service/railway.toml

cp railway-deploy/frontend/Dockerfile frontend/Dockerfile
cp railway-deploy/frontend/nginx.conf frontend/nginx.conf

# STEP 2 — Copy prod application.yml to each Spring Boot service
for svc in weather-ingestion-service heat-processing-service agent-service routing-service notification-service; do
  cp railway-deploy/application-prod.yml $svc/src/main/resources/application-prod.yml
done

# STEP 3 — Commit everything
git add -A
git commit -m "chore: add Railway deployment config (Dockerfiles + railway.toml)"
git push origin main

echo ""
echo "==================================================="
echo "Files committed! Now set up Railway:"
echo "==================================================="
echo ""
echo "1. Go to railway.app → New Project → Deploy from GitHub"
echo "   → Select: Pawanupadhyay10/heatshield"
echo ""
echo "2. Add shared services (click + Add Service):"
echo "   → PostgreSQL (Railway native) — for PostGIS use:"
echo "      image: postgis/postgis:15-3.3"
echo "   → Redis (Railway native)"
echo "   → Kafka: use CloudKarafka free tier (kafka.cloudkarafka.com)"
echo ""
echo "3. For EACH microservice, click + Add Service → GitHub Repo"
echo "   Set ROOT DIRECTORY to the service folder:"
echo "   MS-1: weather-ingestion-service"
echo "   MS-2: heat-processing-service"
echo "   MS-3: agent-service"
echo "   MS-4: routing-service"
echo "   MS-5: notification-service"
echo "   Frontend: frontend"
echo ""
echo "4. Set environment variables for each service:"
echo "   (see ENVIRONMENT_VARIABLES section below)"
echo ""
echo "==================================================="
echo "ENVIRONMENT VARIABLES PER SERVICE"
echo "==================================================="
echo ""
echo "--- ALL SERVICES ---"
echo "SPRING_PROFILES_ACTIVE=prod"
echo "DATABASE_URL=postgresql://... (from Railway Postgres)"
echo "PGUSER=postgres"
echo "PGPASSWORD=... (from Railway Postgres)"
echo "REDIS_URL=redis://... (from Railway Redis)"
echo "KAFKA_BROKER_URL=... (from CloudKarafka)"
echo ""
echo "--- MS-1 ONLY ---"
echo "OWM_API_KEY=50edd8b5054ff77e2a05684cafc2fc66"
echo ""
echo "--- MS-3 ONLY ---"
echo "GEMINI_API_KEY=your_gemini_key"
echo ""
echo "--- FRONTEND ONLY ---"
echo "VITE_MAPBOX_TOKEN=your_mapbox_token"
echo "VITE_MS2_BASE=https://ms2-production.up.railway.app"
echo "VITE_MS3_BASE=https://ms3-production.up.railway.app"
echo "VITE_MS4_BASE=https://ms4-production.up.railway.app"
echo "VITE_MS5_BASE=https://ms5-production.up.railway.app"
