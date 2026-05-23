#!/bin/bash
export OWM_API_KEY=50edd8b5054ff77e2a05684cafc2fc66
export GEMINI_API_KEY=your_gemini_key

cd /workspaces/heatshield/heatshield

echo "Starting all services..."

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
