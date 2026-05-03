package com.heatshield.processing.service;

import com.heatshield.processing.entity.HeatReadingEntity;
import com.heatshield.processing.kafka.RiskEventProducer;
import com.heatshield.processing.model.HeatEvent;
import com.heatshield.processing.model.RiskEvent;
import com.heatshield.processing.repository.HeatReadingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class HeatProcessingService {

    private final HeatReadingRepository heatReadingRepository;
    private final RedisCacheService     redisCacheService;
    private final RiskEventProducer     riskEventProducer;

    private static final List<String> TIER_ORDER =
            List.of("SAFE", "WATCH", "WARNING", "DANGER", "EXTREME");

    public void process(HeatEvent event) {
        log.info("Processing: city={} HI={}°C tier={}",
                event.getCityName(),
                String.format("%.1f", event.getHeatIndex()),
                event.getRiskTier());

        // Step 1: Compute trend
        String trend = computeTrend(event);

        // Step 2: Write to TimescaleDB — own transaction
        saveToTimescaleDB(event, trend);

        // Step 3: Vulnerability estimate (PostGIS wired in Week 4)
        double vulnerabilityScore = estimateVulnerabilityScore(event);
        long   estimatedAtRisk    = estimateAtRiskPopulation(event);
        log.debug("Vulnerability estimate: score={} atRisk={}",
                String.format("%.1f", vulnerabilityScore), estimatedAtRisk);

        // Step 4: Update Redis
        try {
            redisCacheService.updateCityHeatState(
                    event.getCityId(), event.getCityName(),
                    event.getHeatIndex(), event.getRiskTier(),
                    vulnerabilityScore, estimatedAtRisk, trend);
            redisCacheService.storeTierForEscalationTracking(
                    event.getCityId(), event.getRiskTier());
        } catch (Exception e) {
            log.warn("Redis update failed: {}", e.getMessage());
        }

        // Step 5: Tier escalation check
        checkAndPublishRiskEvent(event, vulnerabilityScore, estimatedAtRisk);

        log.info("✓ Processed city={} tier={} trend={}",
                event.getCityName(), event.getRiskTier(), trend);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveToTimescaleDB(HeatEvent event, String trend) {
        heatReadingRepository.save(toEntity(event, trend));
        log.debug("Saved to TimescaleDB: city={}", event.getCityId());
    }

    @Transactional(readOnly = true)
    public String computeTrend(HeatEvent event) {
        return heatReadingRepository
                .findLatestByCityId(event.getCityId())
                .map(prev -> {
                    double delta = event.getHeatIndex() - prev.getHeatIndex();
                    if (delta > 2.0)  return "RISING";
                    if (delta < -2.0) return "FALLING";
                    return "STABLE";
                })
                .orElse("STABLE");
    }

    private void checkAndPublishRiskEvent(HeatEvent event,
                                           double vulnerabilityScore,
                                           long estimatedAtRisk) {
        Optional<String> previousTier =
                redisCacheService.getPreviousTier(event.getCityId());

        boolean isEscalation = previousTier
                .map(prev -> isTierUpgrade(prev, event.getRiskTier()))
                .orElse(event.getRiskTier().equals("DANGER") ||
                        event.getRiskTier().equals("EXTREME"));

        if (isEscalation) {
            String prevTier = previousTier.orElse("UNKNOWN");
            log.info("TIER ESCALATION: city={} {}→{}",
                    event.getCityName(), prevTier, event.getRiskTier());
            try {
                riskEventProducer.publish(RiskEvent.builder()
                        .cityId(event.getCityId())
                        .cityName(event.getCityName())
                        .countryCode(event.getCountryCode())
                        .continentCode(event.getContinentCode())
                        .lat(event.getLat()).lng(event.getLng())
                        .heatIndex(event.getHeatIndex())
                        .previousTier(prevTier)
                        .currentTier(event.getRiskTier())
                        .vulnerabilityScore(vulnerabilityScore)
                        .estimatedAtRisk(estimatedAtRisk)
                        .timestamp(Instant.now().toEpochMilli())
                        .triggerReason("TIER_UPGRADE: " + prevTier +
                                       "→" + event.getRiskTier())
                        .build());
            } catch (Exception e) {
                log.error("Failed to publish RiskEvent: {}", e.getMessage());
            }
        }
    }

    private boolean isTierUpgrade(String old, String next) {
        return TIER_ORDER.indexOf(next) > TIER_ORDER.indexOf(old);
    }

    private double estimateVulnerabilityScore(HeatEvent e) {
        return Math.min(100, (e.getHeatIndex() / 54.0) * 100 * 0.7 + 30);
    }

    private long estimateAtRiskPopulation(HeatEvent e) {
        return switch (e.getCountryCode()) {
            case "IN" -> 500_000L;
            case "EG" -> 300_000L;
            default   -> 100_000L;
        };
    }

    private HeatReadingEntity toEntity(HeatEvent e, String trend) {
        return HeatReadingEntity.builder()
                .time(Instant.ofEpochMilli(e.getTimestamp()))
                .cityId(e.getCityId())
                .cityName(e.getCityName())
                .countryCode(e.getCountryCode())
                .continentCode(e.getContinentCode())
                .lat(e.getLat()).lng(e.getLng())
                .tempC(e.getTempC())
                .feelsLikeC(e.getFeelsLikeC())
                .humidity(e.getHumidity())
                .windSpeedMs(e.getWindSpeedMs())
                .uvIndex(e.getUvIndex())
                .heatIndex(e.getHeatIndex())
                .riskTier(e.getRiskTier())
                .trend(trend)
                .dataSource(e.getDataSource())
                .build();
    }
}
