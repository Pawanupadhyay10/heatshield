package com.heatshield.agent.controller;

import com.heatshield.agent.model.RiskEvent;
import com.heatshield.agent.repository.AgentRunRepository;
import com.heatshield.agent.service.AgentOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentOrchestrator     orchestrator;
    private final AgentRunRepository    agentRunRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        return Map.of(
            "service",             "agent-service",
            "status",              "running",
            "alerts_last_24h",     agentRunRepository.countAlertsDispatchedLast24Hours(),
            "fallbacks_last_hour", agentRunRepository.countFallbacksLastHour()
        );
    }

    @GetMapping("/alert/{cityId}")
    public Map<String, Object> getLatestAlert(@PathVariable String cityId) {
        Map<Object, Object> alert = redisTemplate.opsForHash()
                .entries("alert:latest:" + cityId);
        if (alert.isEmpty())
            return Map.of("cityId", cityId, "status", "no_alert");
        Map<String, Object> result = new HashMap<>();
        alert.forEach((k, v) -> result.put(k.toString(), v));
        return result;
    }

    @GetMapping("/runs/{cityId}")
    public Map<String, Object> getRecentRuns(@PathVariable String cityId) {
        var runs = agentRunRepository.findRecentByCityId(cityId);
        return Map.of("cityId", cityId, "count", runs.size(),
                "runs", runs.stream().map(r -> Map.of(
                    "id", r.getId(),
                    "alertDispatched", r.getAlertDispatched(),
                    "fallbackUsed", r.getFallbackUsed(),
                    "durationMs", r.getTotalDurationMs(),
                    "createdAt", r.getCreatedAt()
                )).toList());
    }

    @PostMapping("/trigger/{cityId}")
    public Map<String, Object> triggerAgent(
            @PathVariable String cityId,
            @RequestParam(defaultValue = "Haridwar") String cityName,
            @RequestParam(defaultValue = "DANGER")   String tier,
            @RequestParam(defaultValue = "WATCH")    String prevTier) {
        log.info("Manual trigger: city={} tier={}", cityName, tier);
        RiskEvent event = RiskEvent.builder()
                .cityId(cityId).cityName(cityName)
                .countryCode("IN").continentCode("AS")
                .lat(29.9457).lng(78.1642)
                .heatIndex(44.5).previousTier(prevTier).currentTier(tier)
                .vulnerabilityScore(72.0).estimatedAtRisk(500_000L)
                .timestamp(Instant.now().toEpochMilli())
                .triggerReason("manual_trigger").build();
        try {
            orchestrator.processRiskEvent(event);
            return Map.of("status", "triggered", "cityId", cityId, "tier", tier);
        } catch (Exception e) {
            return Map.of("status", "error", "message", e.getMessage());
        }
    }
}
