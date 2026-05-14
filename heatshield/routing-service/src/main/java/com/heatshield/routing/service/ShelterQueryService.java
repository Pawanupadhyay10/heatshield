package com.heatshield.routing.service;

import com.heatshield.routing.model.RouteResponse;
import com.heatshield.routing.model.ShelterResult;
import com.heatshield.routing.repository.CoolShelterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * Core service — finds and scores cool shelters near a location.
 *
 * INTERVIEW TALKING POINT:
 * "The key innovation is sorting by exposure score, not distance.
 *  Step 1: PostGIS ST_DWithin finds candidates within radius.
 *  Step 2: RouteExposureScorer computes Σ(HI × distance) per route.
 *  Step 3: Sort ascending — lowest exposure = recommended route.
 *  This is a custom routing cost function you won't find in any library."
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShelterQueryService {

    private final CoolShelterRepository shelterRepository;
    private final RouteExposureScorer   exposureScorer;
    private final RedisCacheService     cacheService;
    private final OsmDataLoader         osmDataLoader;

    // Search radius — 5km (~0.045 degrees at equator)
    private static final double DEFAULT_RADIUS_DEGREES = 0.045;
    private static final double MAX_RADIUS_DEGREES     = 0.135; // 15km fallback

    public RouteResponse findCoolShelters(double lat, double lng,
                                           int maxResults, String cityId) {
        // Check Redis cache first
        var cached = cacheService.getCachedRoute(lat, lng);
        if (cached.isPresent()) {
            log.debug("Serving cached route for lat={} lng={}", lat, lng);
            cached.get().setSource("redis_cache");
            return cached.get();
        }

        double heatIndex = cacheService.getCurrentHeatIndex(cityId);
        String riskTier  = cacheService.getCurrentRiskTier(cityId);

        log.info("Finding shelters: lat={} lng={} HI={} tier={}",
                lat, lng, heatIndex, riskTier);

        // Step 1: PostGIS spatial query
        var shelterEntities = shelterRepository.findNearbyShelters(
                lat, lng, DEFAULT_RADIUS_DEGREES, maxResults * 3);

        // Expand radius if no results found
        if (shelterEntities.isEmpty()) {
            log.warn("No shelters in {}deg radius, expanding to {}deg",
                    DEFAULT_RADIUS_DEGREES, MAX_RADIUS_DEGREES);
            shelterEntities = shelterRepository.findNearbyShelters(
                    lat, lng, MAX_RADIUS_DEGREES, maxResults * 3);
        }

        // Step 2: Score each shelter by heat exposure
        List<ShelterResult> scored = shelterEntities.stream()
                .map(s -> exposureScorer.scoreAndBuild(s, lat, lng, heatIndex))
                .sorted(Comparator.comparingDouble(ShelterResult::getExposureScore))
                .limit(maxResults)
                .toList();

        // Step 3: If still empty, return fallback shelters
        if (scored.isEmpty()) {
            scored = buildFallbackShelters(lat, lng, heatIndex);
            log.warn("No PostGIS shelters found — using fallback");
        }

        RouteResponse response = RouteResponse.builder()
                .originLat(lat)
                .originLng(lng)
                .currentRiskTier(riskTier)
                .currentHeatIndex(heatIndex)
                .shelters(scored)
                .generalAdvice(buildAdvice(heatIndex, riskTier))
                .cachedAt(Instant.now().toEpochMilli())
                .source("postgis_live")
                .build();

        // Cache for 10 minutes
        cacheService.cacheRouteResponse(lat, lng, response);

        log.info("Returning {} shelters for lat={} lng={}",
                scored.size(), lat, lng);
        return response;
    }

    private String buildAdvice(double heatIndex, String tier) {
        return switch (tier) {
            case "EXTREME" -> String.format(
                "LIFE-THREATENING heat (%.1f°C). Seek shelter IMMEDIATELY. " +
                "Do not walk more than 200m in direct sun.", heatIndex);
            case "DANGER" -> String.format(
                "DANGEROUS heat (%.1f°C). Move to cool shelter as soon as possible. " +
                "Travel early morning or after 7pm only.", heatIndex);
            case "WARNING" -> String.format(
                "High heat (%.1f°C). Stay hydrated. " +
                "Limit outdoor activity to before 10am and after 6pm.", heatIndex);
            default -> String.format(
                "Heat index %.1f°C. Stay hydrated and seek shade when possible.",
                heatIndex);
        };
    }

    private List<ShelterResult> buildFallbackShelters(
            double lat, double lng, double heatIndex) {
        return List.of(
            ShelterResult.builder()
                .name("Nearest Government Hospital")
                .shelterType("hospital")
                .lat(lat + 0.01)
                .lng(lng + 0.01)
                .distanceKm(1.5)
                .exposureScore(heatIndex * 1.5)
                .acConfirmed(true)
                .routeAdvice("Head to nearest government hospital for cool shelter.")
                .source("fallback")
                .build(),
            ShelterResult.builder()
                .name("Local Bus Station")
                .shelterType("community_center")
                .lat(lat - 0.005)
                .lng(lng + 0.005)
                .distanceKm(0.8)
                .exposureScore(heatIndex * 0.8)
                .acConfirmed(false)
                .routeAdvice("Bus station provides shaded seating.")
                .source("fallback")
                .build()
        );
    }
}
