package com.heatshield.notification.controller;

import com.heatshield.notification.channel.NotificationChannel;
import com.heatshield.notification.model.AlertCommand;
import com.heatshield.notification.repository.NotificationLogRepository;
import com.heatshield.notification.service.NotificationDispatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationDispatcher    dispatcher;
    private final NotificationLogRepository logRepository;
    private final List<NotificationChannel> channels;
    private final RedisTemplate<String, Object> redisTemplate;

    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        return Map.of(
            "service",           "notification-service",
            "status",            "running",
            "channels",          channels.stream().map(c -> Map.of(
                "name",    c.channelName(),
                "enabled", c.isEnabled()
            )).toList(),
            "sent_last_24h",     logRepository.countSuccessfulLast24Hours(),
            "dlq_last_hour",     logRepository.countDlqLast1Hour()
        );
    }

    @GetMapping("/feed/global")
    public Map<String, Object> getGlobalFeed() {
        var feed = redisTemplate.opsForList()
                .range("alerts:global:feed", 0, 19);
        return Map.of(
            "alerts", feed != null ? feed : List.of(),
            "count",  feed != null ? feed.size() : 0
        );
    }

    @GetMapping("/alert/{cityId}")
    public Map<String, Object> getCityAlert(@PathVariable String cityId) {
        var alert = redisTemplate.opsForHash()
                .entries("alert:live:" + cityId);
        if (alert.isEmpty())
            return Map.of("cityId", cityId, "status", "no_alert");
        Map<String, Object> result = new java.util.HashMap<>();
        alert.forEach((k, v) -> result.put(k.toString(), v));
        return result;
    }

    // Manual trigger for testing
    @PostMapping("/test/{cityId}")
    public Map<String, Object> testDispatch(
            @PathVariable String cityId,
            @RequestParam(defaultValue = "Haridwar") String cityName,
            @RequestParam(defaultValue = "DANGER")   String severity) {

        AlertCommand command = AlertCommand.builder()
                .alertId(UUID.randomUUID().toString())
                .cityId(cityId).cityName(cityName)
                .countryCode("IN").severity(severity)
                .message("Test alert — Heat index 44.5°C. Seek shelter immediately.")
                .heatIndex(44.5).estimatedAtRisk(500_000L)
                .coolShelterInfo("Nearest: Haridwar Railway Station (0.01km)")
                .timestamp(Instant.now().toEpochMilli())
                .build();

        dispatcher.dispatch(command);
        return Map.of("status", "dispatched", "cityId", cityId,
                "severity", severity, "channels", channels.size());
    }
}
