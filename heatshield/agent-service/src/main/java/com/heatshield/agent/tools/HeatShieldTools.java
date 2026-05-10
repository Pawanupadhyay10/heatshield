package com.heatshield.agent.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class HeatShieldTools {

    private final RedisTemplate<String, Object> redisTemplate;
    private final WebClient                     routingWebClient;
    private final ObjectMapper                  objectMapper;

    @Tool("""
        Fetch the current heat index and risk tier for a city.
        Returns heat index in Celsius, risk tier (SAFE/WATCH/WARNING/DANGER/EXTREME),
        trend (RISING/STABLE/FALLING), and estimated population at risk.
        Always call this first to get current conditions.
        """)
    public Map<String, Object> fetchHeatIndex(String cityId) {
        log.info("[TOOL] fetchHeatIndex: cityId={}", cityId);
        long start = System.currentTimeMillis();
        try {
            Map<Object, Object> cached = redisTemplate.opsForHash()
                    .entries("heat:city:" + cityId);
            if (!cached.isEmpty()) {
                Map<String, Object> result = new HashMap<>();
                cached.forEach((k, v) -> result.put(k.toString(), v));
                result.put("tool_duration_ms", System.currentTimeMillis() - start);
                result.put("source", "redis_cache");
                log.info("[TOOL] fetchHeatIndex: HI={} tier={}",
                        result.get("heatIndex"), result.get("riskTier"));
                return result;
            }
            return Map.of("cityId", cityId, "heatIndex", 35.0,
                    "riskTier", "WARNING", "trend", "RISING",
                    "source", "fallback_estimate",
                    "tool_duration_ms", System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("[TOOL] fetchHeatIndex failed: {}", e.getMessage());
            return Map.of("error", e.getMessage(), "cityId", cityId);
        }
    }

    @Tool("""
        Query the vulnerability score and estimated at-risk population
        for a given city. Returns vulnerability score (0-100),
        estimated people at risk. Score above 60 means alert recommended.
        """)
    public Map<String, Object> queryVulnerablePopulation(
            String cityId, String countryCode) {
        log.info("[TOOL] queryVulnerablePopulation: cityId={}", cityId);
        long start = System.currentTimeMillis();
        Map<Object, Object> cached = redisTemplate.opsForHash()
                .entries("heat:city:" + cityId);
        double vulnerabilityScore = cached.containsKey("vulnerabilityScore")
                ? Double.parseDouble(cached.get("vulnerabilityScore").toString())
                : estimateVulnerability(countryCode);
        long atRisk = cached.containsKey("estimatedAtRisk")
                ? Long.parseLong(cached.get("estimatedAtRisk").toString())
                : estimateAtRisk(countryCode);
        return Map.of("cityId", cityId,
                "vulnerabilityScore", vulnerabilityScore,
                "estimatedAtRisk", atRisk,
                "alertRecommended", vulnerabilityScore > 60,
                "tool_duration_ms", System.currentTimeMillis() - start);
    }

    @Tool("""
        Predict dangerous heat windows for the next 24 hours.
        Returns predicted peak heat index, peak hour, and safe outdoor windows.
        Use this to include timing advice in alert messages.
        """)
    public Map<String, Object> predictHeatWindow(
            String cityId, double lat, double lng) {
        log.info("[TOOL] predictHeatWindow: cityId={}", cityId);
        long start = System.currentTimeMillis();
        Object hiObj = redisTemplate.opsForHash()
                .get("heat:city:" + cityId, "heatIndex");
        double currentHI = hiObj != null
                ? Double.parseDouble(hiObj.toString()) : 35.0;
        return Map.of("cityId", cityId,
                "currentHI", currentHI,
                "peakHeatIndex", currentHI * 1.15,
                "peakHour", "14:00 local time",
                "safeWindows", List.of("06:00-08:00", "19:00-21:00"),
                "advice", "Avoid outdoor activity between 11:00-17:00",
                "tool_duration_ms", System.currentTimeMillis() - start);
    }

    @Tool("""
        Find the nearest cool shelters for a given location.
        Returns top 3 shelters with names, types, and estimated distance.
        Always include shelter info in alert messages.
        """)
    public Map<String, Object> findCoolCorridors(
            String cityId, double lat, double lng) {
        log.info("[TOOL] findCoolCorridors: cityId={}", cityId);
        long start = System.currentTimeMillis();
        try {
            Map response = routingWebClient.get()
                    .uri(u -> u.path("/api/v1/routes/cool-shelters")
                            .queryParam("lat", lat)
                            .queryParam("lng", lng)
                            .queryParam("maxResults", 3).build())
                    .retrieve().bodyToMono(Map.class)
                    .block(Duration.ofSeconds(3));
            if (response != null) {
                response.put("tool_duration_ms", System.currentTimeMillis() - start);
                return response;
            }
        } catch (Exception e) {
            log.warn("[TOOL] Routing service unavailable: {}", e.getMessage());
        }
        return Map.of("cityId", cityId,
                "shelters", List.of(
                    Map.of("name", "Nearest Government Hospital",
                           "type", "hospital", "distanceKm", 1.5),
                    Map.of("name", "Local Metro Station",
                           "type", "metro", "distanceKm", 0.8),
                    Map.of("name", "Shopping Mall",
                           "type", "mall", "distanceKm", 2.1)),
                "advice", "Seek air-conditioned shelter immediately",
                "source", "fallback",
                "tool_duration_ms", System.currentTimeMillis() - start);
    }

    @Tool("""
        Dispatch a heat alert to registered users in the city.
        Only call this if heatIndex > 41 AND vulnerabilityScore > 50.
        Severity must be: WATCH, WARNING, DANGER, or EXTREME.
        DO NOT call if conditions are mild or improving.
        """)
    public Map<String, Object> dispatchAlert(
            String cityId, String cityName, String severity,
            String alertMessage, double heatIndex, long estimatedAtRisk) {
        log.info("[TOOL] dispatchAlert: city={} severity={} HI={}",
                cityName, severity, heatIndex);
        String dedupKey = "alert:sent:" + cityId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(dedupKey))) {
            log.info("[TOOL] Alert suppressed — dedup active: {}", cityId);
            return Map.of("dispatched", false,
                    "reason", "dedup_window_active");
        }
        List<String> valid = List.of("WATCH","WARNING","DANGER","EXTREME");
        if (!valid.contains(severity))
            return Map.of("dispatched", false,
                    "reason", "invalid_severity: " + severity);

        redisTemplate.opsForValue().set(dedupKey, "sent", Duration.ofMinutes(60));
        Map<String, Object> alertData = new HashMap<>();
        alertData.put("cityId", cityId);
        alertData.put("cityName", cityName);
        alertData.put("severity", severity);
        alertData.put("message", alertMessage);
        alertData.put("heatIndex", heatIndex);
        alertData.put("estimatedAtRisk", estimatedAtRisk);
        alertData.put("timestamp", Instant.now().toEpochMilli());
        redisTemplate.opsForHash().putAll("alert:latest:" + cityId, alertData);
        redisTemplate.expire("alert:latest:" + cityId, Duration.ofHours(2));
        log.info("[TOOL] Alert dispatched: city={} severity={}", cityName, severity);
        return Map.of("dispatched", true, "severity", severity,
                "estimatedAtRisk", estimatedAtRisk,
                "nextAlertAllowedIn", "60 minutes");
    }

    private double estimateVulnerability(String cc) {
        return switch (cc) { case "IN" -> 72.0; case "EG" -> 65.0; default -> 50.0; };
    }
    private long estimateAtRisk(String cc) {
        return switch (cc) { case "IN" -> 500_000L; case "EG" -> 200_000L; default -> 100_000L; };
    }
}
