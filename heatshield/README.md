# 🌡️ HeatShield — Global Urban Heat Vulnerability Platform

**Live Demo:** https://frontend-production-947fb.up.railway.app

## Architecture
5 Spring Boot microservices + React frontend deployed on Railway

- **MS-1** Weather Ingestion — OWM API + Steadman Heat Index
- **MS-2** Heat Processing — Kafka consumer + TimescaleDB + Redis  
- **MS-3** AI Agent — LangChain4j ReAct + Gemini + circuit breaker
- **MS-4** Routing — PostGIS exposure-weighted shelter routing
- **MS-5** Notifications — Strategy pattern multi-channel alerts
- **Frontend** — Vite + React + Zustand + React Query + Mapbox GL

## Key Engineering Decisions
- Kafka partitioned by continent for independent scaling
- Exposure-weighted routing: Σ(HeatIndex × distance_km)
- LLM fallback guarantees 100% alert delivery
- Fixed Rothfusz formula for low-humidity cities (RH < 40%)

## Tech Stack
Java 21 · Spring Boot 3.2 · Kafka · PostGIS · TimescaleDB · Redis · LangChain4j · Gemini · React · Vite · Zustand · Mapbox GL · Railway · Supabase
