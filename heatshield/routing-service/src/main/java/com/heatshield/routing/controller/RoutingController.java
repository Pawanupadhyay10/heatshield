package com.heatshield.routing.controller;

import com.heatshield.routing.model.RouteResponse;
import com.heatshield.routing.service.OsmDataLoader;
import com.heatshield.routing.service.ShelterQueryService;
import com.heatshield.routing.repository.CoolShelterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API for routing service.
 *
 * GET /api/v1/routes/cool-shelters  — called by MS-3 agent tool
 * GET /api/v1/routes/status         — health + shelter count
 * POST /api/v1/routes/seed          — manually trigger OSM seed
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/routes")
@RequiredArgsConstructor
public class RoutingController {

    private final ShelterQueryService   shelterQueryService;
    private final CoolShelterRepository shelterRepository;
    private final OsmDataLoader         osmDataLoader;

    /**
     * Main endpoint — called by MS-3 agent's findCoolCorridors tool.
     *
     * Returns nearest cool shelters sorted by heat exposure score.
     * Results cached in Redis for 10 minutes.
     */
    @GetMapping("/cool-shelters")
    public RouteResponse getCoolShelters(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "3")    int    maxResults,
            @RequestParam(defaultValue = "unknown") String cityId) {

        log.info("Cool shelter request: lat={} lng={} maxResults={} city={}",
                lat, lng, maxResults, cityId);
        return shelterQueryService.findCoolShelters(lat, lng, maxResults, cityId);
    }

    /**
     * Status endpoint — shelter count + service health.
     */
    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        long total = shelterRepository.countAllShelters();
        long india = shelterRepository.countByCountryCode("IN");
        return Map.of(
            "service",          "routing-service",
            "status",           "running",
            "total_shelters",   total,
            "india_shelters",   india,
            "algorithm",        "exposure-weighted: Σ(HeatIndex × distance_km)"
        );
    }

    /**
     * Manually seed shelter data — for dev/testing.
     */
    @PostMapping("/seed")
    public Map<String, Object> seedShelters() {
        log.info("Manual shelter seed triggered");
        osmDataLoader.seedHardcodedShelters();
        return Map.of(
            "status",  "seeded",
            "count",   shelterRepository.countAllShelters()
        );
    }

    /**
     * Test endpoint for Haridwar specifically — for demo.
     */
    @GetMapping("/haridwar")
    public RouteResponse getHaridwarShelters() {
        return shelterQueryService.findCoolShelters(
                29.9457, 78.1642, 3, "city-haridwar");
    }
}
