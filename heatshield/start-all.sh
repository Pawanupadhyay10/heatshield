#!/bin/bash

# Load environment variables from .env file
if [ -f .env ]; then
  set -a
  source .env
  set +a
else
  echo "❌ Error: .env file not found!"
  echo "📋 Please copy .env.example to .env and fill in your API keys:"
  echo "   cp .env.example .env"
  exit 1
fi

# Validate required environment variables
if [ -z "$OWM_API_KEY" ]; then
  echo "❌ Error: OWM_API_KEY is not set in .env file"
  exit 1
fi

cd /workspaces/heatshield/heatshield

echo "✅ Starting all services..."

# Kill existing
for port in 8081 8082 8083 8084 8085; do
  kill -9 $(lsof -t -i:$port) 2>/dev/null || true
done
sleep 2

# Start each service in background with logs
nohup mvn -f weather-ingestion-service/pom.xml spring-boot:run > /tmp/ms1.log 2>&1 &
echo "MS-1 starting (PID $!)"
sleep 5

nohup mvn -f heat-processing-service/pom.xml spring-boot:run > /tmp/ms2.log 2>&1 &
echo "MS-2 starting (PID $!)"
sleep 5

nohup mvn -f agent-service/pom.xml spring-boot:run > /tmp/ms3.log 2>&1 &
echo "MS-3 starting (PID $!)"
sleep 5

nohup mvn -f routing-service/pom.xml spring-boot:run > /tmp/ms4.log 2>&1 &
echo "MS-4 starting (PID $!)"
sleep 5

nohup mvn -f notification-service/pom.xml spring-boot:run > /tmp/ms5.log 2>&1 &
echo "MS-5 starting (PID $!)"

echo "All services starting. Check logs:"
echo "  tail -f /tmp/ms1.log"
echo "  tail -f /tmp/ms2.log"
