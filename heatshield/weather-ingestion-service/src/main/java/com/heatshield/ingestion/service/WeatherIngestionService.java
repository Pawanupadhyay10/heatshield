package com.heatshield.ingestion.service;

import com.heatshield.ingestion.kafka.HeatEventProducer;
import com.heatshield.ingestion.model.HeatEvent;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * MS-1: Weather Ingestion Service — core polling logic.
 *
 * Flow:
 *   @Scheduled every 15 min
 *     → load active cities from Redis (pre-loaded from PostGIS on startup)
 *     → for each city, call OpenWeatherMap API (with circuit breaker)
 *     → compute Heat Index (Steadman formula)
 *     → classify risk tier
 *     → publish HeatEvent to Kafka topic: heat-events
 *
 * INTERVIEW TALKING POINT:
 * "I poll 500 cities every 15 min = ~33 req/s sustained.
 *  OWM free tier allows 60 req/min so I batch with a 1-second
 *  delay between calls and use Redis to cache responses for
 *  5 minutes, so rapid UI requests don't re-trigger API calls."
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherIngestionService {

    private final WebClient owmWebClient;
    private final HeatEventProducer producer;
    private final HeatIndexCalculator calculator;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${owm.api-key}")
    private String owmApiKey;

    private static final String CACHE_KEY_PREFIX = "heat:raw:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    /**
     * Polls every 15 minutes for all active cities.
     * fixedDelay ensures the next run starts 15 min after the
     * previous one COMPLETES (prevents overlap on slow runs).
     */
    @Scheduled(fixedDelayString = "${ingestion.poll-interval-ms:900000}",
               initialDelay = 5000)
    public void pollAllCities() {
        List<CityRef> cities = getActiveCities();
        log.info("Starting weather poll for {} cities", cities.size());

        int success = 0, failed = 0;
        for (CityRef city : cities) {
            try {
                processCity(city);
                success++;
                // Rate limit: OWM free tier = 60 req/min
                Thread.sleep(1100);
            } catch (Exception e) {
                log.error("Failed to process city {}: {}", city.name(), e.getMessage());
                failed++;
            }
        }
        log.info("Poll complete. Success: {}, Failed: {}", success, failed);
    }

    @CircuitBreaker(name = "owm-api", fallbackMethod = "fallbackFromCache")
    @Retry(name = "owm-api")
    public void processCity(CityRef city) {
        String cacheKey = CACHE_KEY_PREFIX + city.id();

        // Check Redis cache first
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("Cache hit for city {}", city.name());
            return; // Already processed recently
        }

        // Call OpenWeatherMap Current Weather API
        Map<String, Object> response = owmWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/data/2.5/weather")
                        .queryParam("lat", city.lat())
                        .queryParam("lon", city.lng())
                        .queryParam("appid", owmApiKey)
                        .queryParam("units", "metric")
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block(Duration.ofSeconds(10));

        HeatEvent event = buildHeatEvent(city, response);

        // Cache raw response (prevent duplicate API calls)
        redisTemplate.opsForValue().set(cacheKey, event, CACHE_TTL);

        // Publish to Kafka
        producer.publish(event);

        log.info("Published HeatEvent: city={}, HI={}°C, tier={}",
                city.name(), String.format("%.1f", event.getHeatIndex()), event.getRiskTier());
    }

    @SuppressWarnings("unchecked")
    private HeatEvent buildHeatEvent(CityRef city, Map<String, Object> response) {
        Map<String, Object> main = (Map<String, Object>) response.get("main");
        Map<String, Object> wind = (Map<String, Object>) response.getOrDefault("wind", Map.of());

        double tempC     = toDouble(main.get("temp"));
        double humidity  = toDouble(main.get("humidity"));
        double feelsLike = toDouble(main.get("feels_like"));
        double windSpeed = toDouble(wind.getOrDefault("speed", 0));

        double heatIndex = calculator.calculate(tempC, humidity);
        String riskTier  = calculator.classifyRisk(heatIndex);

        return HeatEvent.builder()
                .cityId(city.id())
                .cityName(city.name())
                .countryCode(city.countryCode())
                .continentCode(city.continentCode())
                .lat(city.lat())
                .lng(city.lng())
                .tempC(tempC)
                .feelsLikeC(feelsLike)
                .humidity(humidity)
                .windSpeedMs(windSpeed)
                .heatIndex(heatIndex)
                .riskTier(riskTier)
                .timestamp(Instant.now().toEpochMilli())
                .dataSource("openweathermap")
                .build();
    }

    // Circuit breaker fallback — serve stale cache if OWM is down
    public void fallbackFromCache(CityRef city, Exception ex) {
        log.warn("OWM circuit open for city {}. Reason: {}", city.name(), ex.getMessage());
        String cacheKey = CACHE_KEY_PREFIX + city.id();
        Object stale = redisTemplate.opsForValue().get(cacheKey);
        if (stale instanceof HeatEvent event) {
            log.warn("Serving stale cache for city {}", city.name());
            producer.publish(event);
        }
    }

    private List<CityRef> getActiveCities() {
        // In Week 1: return hardcoded pilot cities matching DB seed
        // Week 2+: load dynamically from PostGIS via REST call to routing-service
        return List.of(
            new CityRef("city-haridwar", "Haridwar", "IN", "AS", 29.9457, 78.1642),
            new CityRef("city-delhi",    "New Delhi", "IN", "AS", 28.6139, 77.2090),
            new CityRef("city-mumbai",   "Mumbai",    "IN", "AS", 18.9667, 72.8777),
            new CityRef("city-cairo",    "Cairo",     "EG", "AF", 30.0444, 31.2357),
            new CityRef("city-madrid",   "Madrid",    "ES", "EU", 40.4168, -3.7038)
        );
    }

    private double toDouble(Object val) {
        if (val instanceof Number n) return n.doubleValue();
        return 0.0;
    }

    // Simple value record for city reference data
    public record CityRef(String id, String name, String countryCode,
                          String continentCode, double lat, double lng) {}
}
