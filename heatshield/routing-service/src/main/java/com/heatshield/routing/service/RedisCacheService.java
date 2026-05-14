package com.heatshield.routing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heatshield.routing.model.RouteResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper                  objectMapper;

    private static final Duration ROUTE_TTL    = Duration.ofMinutes(10);
    private static final String   ROUTE_PREFIX = "route:shelters:";

    public void cacheRouteResponse(double lat, double lng,
                                    RouteResponse response) {
        String key = buildKey(lat, lng);
        try {
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(key, json, ROUTE_TTL);
            log.debug("Cached route response: key={}", key);
        } catch (Exception e) {
            log.warn("Failed to cache route: {}", e.getMessage());
        }
    }

    public Optional<RouteResponse> getCachedRoute(double lat, double lng) {
        String key = buildKey(lat, lng);
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                RouteResponse response = objectMapper.readValue(
                        cached.toString(), RouteResponse.class);
                log.debug("Cache hit: key={}", key);
                return Optional.of(response);
            }
        } catch (Exception e) {
            log.warn("Cache read failed: {}", e.getMessage());
        }
        return Optional.empty();
    }

    public double getCurrentHeatIndex(String cityId) {
        try {
            Object hi = redisTemplate.opsForHash()
                    .get("heat:city:" + cityId, "heatIndex");
            return hi != null ? Double.parseDouble(hi.toString()) : 35.0;
        } catch (Exception e) {
            return 35.0; // safe default
        }
    }

    public String getCurrentRiskTier(String cityId) {
        try {
            Object tier = redisTemplate.opsForHash()
                    .get("heat:city:" + cityId, "riskTier");
            return tier != null ? tier.toString() : "WATCH";
        } catch (Exception e) {
            return "WATCH";
        }
    }

    // Round to 3 decimal places (~111m precision) for cache key
    private String buildKey(double lat, double lng) {
        return String.format("%s%.3f:%.3f",
                ROUTE_PREFIX,
                Math.round(lat * 1000.0) / 1000.0,
                Math.round(lng * 1000.0) / 1000.0);
    }
}
