package com.heatshield.agent.service;

import com.heatshield.agent.model.RiskEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FallbackAlertService {

    private final RedisTemplate<String, Object> redisTemplate;

    public boolean evaluate(RiskEvent event) {
        String tier = event.getCurrentTier();
        double vuln = event.getVulnerabilityScore();
        double hi   = event.getHeatIndex();

        if ("EXTREME".equals(tier)) {
            return dispatch(event, "EXTREME HEAT EMERGENCY: Heat index "
                    + String.format("%.1f", hi) + "C. Seek cool shelter immediately.");
        }
        if ("DANGER".equals(tier) && vuln > 50) {
            return dispatch(event, "HEAT DANGER WARNING: Heat index "
                    + String.format("%.1f", hi) + "C in " + event.getCityName()
                    + ". Stay indoors 11am-5pm.");
        }
        if ("WARNING".equals(tier) && vuln > 70) {
            return dispatch(event, "Heat Warning in " + event.getCityName()
                    + ". Heat index " + String.format("%.1f", hi)
                    + "C. Limit outdoor exposure.");
        }
        log.info("Fallback: no alert for {} tier={}", event.getCityName(), tier);
        return false;
    }

    private boolean dispatch(RiskEvent event, String message) {
        String dedupKey = "alert:sent:" + event.getCityId();
        if (Boolean.TRUE.equals(redisTemplate.hasKey(dedupKey))) {
            log.info("Fallback suppressed — dedup active: {}", event.getCityId());
            return false;
        }
        Map<String, Object> alert = new HashMap<>();
        alert.put("cityId", event.getCityId());
        alert.put("cityName", event.getCityName());
        alert.put("severity", event.getCurrentTier());
        alert.put("message", message);
        alert.put("heatIndex", event.getHeatIndex());
        alert.put("estimatedAtRisk", event.getEstimatedAtRisk());
        alert.put("timestamp", Instant.now().toEpochMilli());
        alert.put("source", "fallback_rules");
        redisTemplate.opsForHash().putAll("alert:latest:" + event.getCityId(), alert);
        redisTemplate.expire("alert:latest:" + event.getCityId(), Duration.ofHours(2));
        redisTemplate.opsForValue().set(dedupKey, "sent", Duration.ofMinutes(60));
        log.warn("FALLBACK ALERT: city={} severity={}", event.getCityName(), event.getCurrentTier());
        return true;
    }
}
