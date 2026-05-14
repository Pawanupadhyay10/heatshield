package com.heatshield.notification.channel;

import com.heatshield.notification.model.AlertCommand;
import com.heatshield.notification.model.DeliveryResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis channel — stores alert for React frontend live feed.
 * ALWAYS enabled — no external API needed.
 * This is what the WebSocket endpoint reads for real-time updates.
 *
 * INTERVIEW TALKING POINT:
 * "Even if SMS and email fail, the Redis channel always succeeds.
 *  The React frontend reads from Redis via WebSocket to show
 *  live alerts on the global map. This is the minimum viable
 *  delivery guarantee."
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisAlertChannel implements NotificationChannel {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public String channelName() { return "redis"; }

    @Override
    public boolean isEnabled() { return true; }

    @Override
    public DeliveryResult send(AlertCommand command) {
        long start = System.currentTimeMillis();
        try {
            // Store in live alert feed (read by React frontend)
            String alertKey = "alert:live:" + command.getCityId();
            Map<String, Object> alertData = new HashMap<>();
            alertData.put("alertId",          command.getAlertId());
            alertData.put("cityId",           command.getCityId());
            alertData.put("cityName",         command.getCityName());
            alertData.put("severity",         command.getSeverity());
            alertData.put("message",          command.getMessage());
            alertData.put("heatIndex",        command.getHeatIndex());
            alertData.put("estimatedAtRisk",  command.getEstimatedAtRisk());
            alertData.put("coolShelterInfo",  command.getCoolShelterInfo());
            alertData.put("timestamp",        Instant.now().toEpochMilli());
            alertData.put("countryCode",      command.getCountryCode());

            redisTemplate.opsForHash().putAll(alertKey, alertData);
            redisTemplate.expire(alertKey, Duration.ofHours(6));

            // Also push to global alerts list (last 100 alerts)
            String globalKey = "alerts:global:feed";
            redisTemplate.opsForList().leftPush(globalKey,
                    command.getCityName() + ":" + command.getSeverity());
            redisTemplate.opsForList().trim(globalKey, 0, 99);
            redisTemplate.expire(globalKey, Duration.ofHours(24));

            log.info("[REDIS] Alert stored: city={} severity={}",
                    command.getCityName(), command.getSeverity());

            return DeliveryResult.builder()
                    .channel("redis").success(true)
                    .messageId(alertKey)
                    .durationMs(System.currentTimeMillis() - start)
                    .build();
        } catch (Exception e) {
            log.error("[REDIS] Failed: {}", e.getMessage());
            return DeliveryResult.builder()
                    .channel("redis").success(false)
                    .errorMessage(e.getMessage())
                    .durationMs(System.currentTimeMillis() - start)
                    .build();
        }
    }
}
