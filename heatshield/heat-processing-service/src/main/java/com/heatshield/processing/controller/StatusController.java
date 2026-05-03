package com.heatshield.processing.controller;

import com.heatshield.processing.repository.HeatReadingRepository;
import com.heatshield.processing.service.RedisCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/status")
@RequiredArgsConstructor
public class StatusController {

    private final RedisCacheService     redisCacheService;
    private final HeatReadingRepository heatReadingRepository;

    @GetMapping("/city/{cityId}")
    public Map<String, Object> getCityStatus(@PathVariable String cityId) {
        Map<Object, Object> cached = redisCacheService.getCityHeatState(cityId);
        if (cached.isEmpty()) {
            return Map.of("cityId", cityId, "status", "no_data");
        }
        return Map.of("cityId", cityId, "status", "ok", "heatState", cached);
    }

    @GetMapping("/high-risk")
    public Map<String, Object> getHighRiskCities() {
        var cities = heatReadingRepository.findCurrentHighRiskCities();
        return Map.of("count", cities.size(), "cities", cities.stream().map(c ->
            Map.of("cityId", c.getCityId(), "cityName", c.getCityName(),
                   "heatIndex", c.getHeatIndex(), "riskTier", c.getRiskTier())
        ).toList());
    }

    @GetMapping("/metrics")
    public Map<String, Object> getMetrics() {
        return Map.of(
            "readings_last_24h", heatReadingRepository.countLast24Hours(),
            "service", "heat-processing-service",
            "status", "running"
        );
    }
}
