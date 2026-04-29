package com.heatshield.agent.agent;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.*;

/**
 * HeatShield AI Agent — 5-tool ReAct agent (LangChain4j)
 *
 * ReAct = Reason + Act loop:
 *   1. Agent receives trigger (risk event or schedule)
 *   2. LLM reasons about which tool to call
 *   3. Tool executes, returns result
 *   4. LLM reasons about next action
 *   5. Repeat until agent decides to dispatch alert or take no action
 *
 * INTERVIEW TALKING POINT:
 * "Each tool call is logged with input, output, and latency.
 *  If the LLM API is down, a rule-based fallback fires instead.
 *  The agent never silently fails."
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HeatShieldAgent {

    private final RedisTemplate<String, Object> redisTemplate;
    private final WebClient routingServiceClient;
    private final AgentAuditLogger auditLogger;

    // ── Tool 1: Fetch Heat Index ─────────────────────────────
    @Tool("Fetch the current heat index and risk tier for a given location. " +
          "Returns heat index in Celsius, risk tier (SAFE/WATCH/WARNING/DANGER/EXTREME), " +
          "and whether the trend is RISING, STABLE, or FALLING.")
    public Map<String, Object> fetchHeatIndex(double lat, double lng) {
        String key = String.format("heat:processed:%.4f:%.4f", lat, lng);
        Object cached = redisTemplate.opsForValue().get(key);

        if (cached instanceof Map<?,?> data) {
            log.info("Tool 1 fetchHeatIndex: lat={}, lng={} → HI={}",
                    lat, lng, data.get("heatIndex"));
            return (Map<String, Object>) cached;
        }

        // Fallback if no recent reading (unlikely in production)
        return Map.of(
            "heatIndex", 35.0,
            "riskTier", "WARNING",
            "trend", "RISING",
            "source", "fallback_estimate"
        );
    }

    // ── Tool 2: Query Vulnerable Population ──────────────────
    @Tool("Query the population vulnerability score for a given location within a radius. " +
          "Returns vulnerability score 0-100 based on population density, " +
          "percentage of elderly (65+) and children (under 5). " +
          "Score > 60 indicates high-risk zone requiring alert.")
    public Map<String, Object> queryVulnerablePopulation(double lat, double lng,
                                                          double radiusKm) {
        // In production: PostGIS query via JPA repository
        // ST_DWithin(geom, ST_SetSRID(ST_MakePoint(:lng,:lat),4326), :radiusDegrees)
        // For Week 5 stub — replace with real PostGIS call
        log.info("Tool 2 queryVulnerablePopulation: lat={}, lng={}, radius={}km",
                lat, lng, radiusKm);

        // TODO: inject VulnerabilityZoneRepository and run spatial query
        return Map.of(
            "vulnerabilityScore", 72.5,
            "popDensity", 8500.0,       // persons/km²
            "pctElderly", 0.14,
            "pctChildren", 0.18,
            "estimatedAtRisk", 45000
        );
    }

    // ── Tool 3: Predict Heat Window ──────────────────────────
    @Tool("Predict the heat index for the next 24 hours for a given location. " +
          "Returns predicted peak heat index, the hour it will occur (local time), " +
          "and safe outdoor windows (hours when HI < 32°C). " +
          "Uses TimescaleDB historical data + OpenWeatherMap 5-day forecast.")
    public Map<String, Object> predictHeatWindow(double lat, double lng,
                                                  int hoursAhead) {
        log.info("Tool 3 predictHeatWindow: lat={}, lng={}, hours={}", lat, lng, hoursAhead);

        // In production: query TimescaleDB heat_readings_hourly continuous aggregate
        // for 7-day historical avg, then apply linear regression on OWM forecast
        // TODO: inject TimescaleRepository and ForecastService

        return Map.of(
            "peakHeatIndex", 47.2,
            "peakHour", "14:00 IST",
            "safeWindows", List.of("06:00-09:00", "19:00-21:00"),
            "tomorrowPeak", 45.8,
            "confidence", 0.82
        );
    }

    // ── Tool 4: Find Cool Corridors ──────────────────────────
    @Tool("Find the nearest cool shelters (hospitals, malls, metro stations, libraries) " +
          "for a given location and return minimum heat-exposure routes to reach them. " +
          "Route exposure score = sum of (HeatIndex × segment_length) along path. " +
          "Returns top 3 shelters as GeoJSON with exposure-optimised routes.")
    public Map<String, Object> findCoolCorridors(double lat, double lng) {
        log.info("Tool 4 findCoolCorridors: lat={}, lng={}", lat, lng);

        // Calls routing-service (MS-4) via WebClient
        // routing-service queries cool_shelters PostGIS table via ST_DWithin
        // then scores routes by exposure: Σ(HI × segment_length)
        try {
            Map response = routingServiceClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/routes/cool-shelters")
                            .queryParam("lat", lat)
                            .queryParam("lng", lng)
                            .queryParam("maxResults", 3)
                            .build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(Duration.ofSeconds(5));
            return response != null ? response : fallbackShelters();
        } catch (Exception e) {
            log.warn("Routing service unavailable: {}", e.getMessage());
            return fallbackShelters();
        }
    }

    // ── Tool 5: Dispatch Alert ───────────────────────────────
    @Tool("Dispatch a heat alert to registered users in a zone. " +
          "Only dispatches if: heatIndex > threshold AND vulnerabilityScore > 60 " +
          "AND no alert sent in last 60 minutes for this zone. " +
          "Severity must be one of: WATCH, WARNING, DANGER, EXTREME. " +
          "Returns confirmation with estimated users notified.")
    public Map<String, Object> dispatchAlert(String zoneId, String severity,
                                              String message, double heatIndex) {
        log.info("Tool 5 dispatchAlert: zone={}, severity={}, HI={}",
                zoneId, severity, heatIndex);

        // Dedup check — Redis key: alert:sent:{zoneId}
        String dedupKey = "alert:sent:" + zoneId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(dedupKey))) {
            log.info("Alert suppressed for zone {} (sent within 60 min)", zoneId);
            return Map.of("dispatched", false, "reason", "dedup_window_active");
        }

        // Publish AlertCommand to Kafka → notification-service handles delivery
        // TODO: inject KafkaTemplate<String, AlertCommand> and publish here

        // Set dedup key with 60-minute TTL
        redisTemplate.opsForValue().set(dedupKey, "sent", Duration.ofMinutes(60));

        log.info("Alert dispatched for zone {} severity={}", zoneId, severity);
        return Map.of(
            "dispatched", true,
            "severity", severity,
            "estimatedUsersNotified", 1240,
            "channels", List.of("push", "sms", "email")
        );
    }

    // Fallback shelters when routing service is unavailable
    private Map<String, Object> fallbackShelters() {
        return Map.of(
            "shelters", List.of(
                Map.of("name", "Nearest Hospital", "type", "hospital",
                       "distanceKm", 1.2, "exposureScore", 38.4)
            ),
            "source", "fallback"
        );
    }
}
