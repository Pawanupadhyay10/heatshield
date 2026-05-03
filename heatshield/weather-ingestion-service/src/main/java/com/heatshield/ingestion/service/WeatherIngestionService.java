package com.heatshield.ingestion.service;

import com.heatshield.ingestion.kafka.HeatEventProducer;
import com.heatshield.ingestion.model.HeatEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class WeatherIngestionService {

    private final WebClient owmWebClient;
    private final HeatEventProducer producer;
    private final HeatIndexCalculator calculator;

    @Value("${owm.api-key:dummy-key}")
    private String owmApiKey;

    private static final String CACHE_KEY_PREFIX = "heat:raw:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    // Explicit @Qualifier so Spring matches the exact bean name
    public WeatherIngestionService(
            @Qualifier("owmWebClient") WebClient owmWebClient,
            HeatEventProducer producer,
            HeatIndexCalculator calculator) {
        this.owmWebClient = owmWebClient;
        this.producer = producer;
        this.calculator = calculator;
    }

    @Scheduled(fixedDelayString = "${ingestion.poll-interval-ms:900000}",
               initialDelay = 5000)
    public void pollAllCities() {
        List<CityRef> cities = getActiveCities();
        log.info("Starting weather poll for {} cities", cities.size());

        for (CityRef city : cities) {
            try {
                processCity(city);
                Thread.sleep(1100); // OWM rate limit: 60 req/min
            } catch (Exception e) {
                log.error("Failed city {}: {}", city.name(), e.getMessage());
            }
        }
    }

    public void processCity(CityRef city) {
        Map response = owmWebClient.get()
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

        if (response == null) {
            log.warn("Null response for city {}", city.name());
            return;
        }

        HeatEvent event = buildHeatEvent(city, response);
        producer.publish(event);

        log.info("Published → city={} HI={}°C tier={}",
                city.name(),
                String.format("%.1f", event.getHeatIndex()),
                event.getRiskTier());
    }

    @SuppressWarnings("unchecked")
    private HeatEvent buildHeatEvent(CityRef city, Map<String, Object> response) {
        Map<String, Object> main = (Map<String, Object>) response.get("main");
        Map<String, Object> wind = (Map<String, Object>) response.getOrDefault("wind", Map.of());

        double tempC    = toDouble(main.get("temp"));
        double humidity = toDouble(main.get("humidity"));
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

    private List<CityRef> getActiveCities() {
        return List.of(
            new CityRef("city-haridwar", "Haridwar",  "IN", "AS", 29.9457,  78.1642),
            new CityRef("city-delhi",    "New Delhi", "IN", "AS", 28.6139,  77.2090),
            new CityRef("city-mumbai",   "Mumbai",    "IN", "AS", 18.9667,  72.8777),
            new CityRef("city-cairo",    "Cairo",     "EG", "AF", 30.0444,  31.2357),
            new CityRef("city-madrid",   "Madrid",    "ES", "EU", 40.4168,  -3.7038)
        );
    }

    private double toDouble(Object val) {
        if (val instanceof Number n) return n.doubleValue();
        return 0.0;
    }

    public record CityRef(String id, String name, String countryCode,
                          String continentCode, double lat, double lng) {}
}
