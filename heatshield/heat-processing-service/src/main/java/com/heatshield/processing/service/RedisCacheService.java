package com.heatshield.processing.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final Duration CITY_TTL    = Duration.ofMinutes(20);
    private static final Duration TIER_TTL    = Duration.ofHours(2);
    private static final String   CITY_PREFIX = "heat:city:";
    private static final String   TIER_PREFIX = "heat:tier:prev:";

    public void updateCityHeatState(String cityId, String cityName,
                                     double heatIndex, String riskTier,
                                     double vulnerabilityScore,
                                     long estimatedAtRisk, String trend) {
        String key = CITY_PREFIX + cityId;
        Map<String, Object> state = new HashMap<>();
        state.put("cityId",             cityId);
        state.put("cityName",           cityName);
        state.put("heatIndex",          heatIndex);
        state.put("riskTier",           riskTier);
        state.put("vulnerabilityScore", vulnerabilityScore);
        state.put("estimatedAtRisk",    estimatedAtRisk);
        state.put("trend",              trend);
        state.put("updatedAt",          System.currentTimeMillis());
        redisTemplate.opsForHash().putAll(key, state);
        redisTemplate.expire(key, CITY_TTL);
        log.debug("Redis updated: city={} HI={}°C tier={}", cityId,
                String.format("%.1f", heatIndex), riskTier);
    }

    public Optional<String> getPreviousTier(String cityId) {
        Object tier = redisTemplate.opsForValue().get(TIER_PREFIX + cityId);
        return Optional.ofNullable(tier).map(Object::toString);
    }

    public void storeTierForEscalationTracking(String cityId, String tier) {
        redisTemplate.opsForValue().set(TIER_PREFIX + cityId, tier, TIER_TTL);
    }

    public Map<Object, Object> getCityHeatState(String cityId) {
        return redisTemplate.opsForHash().entries(CITY_PREFIX + cityId);
    }

    public boolean hasCityState(String cityId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(CITY_PREFIX + cityId));
    }
}
