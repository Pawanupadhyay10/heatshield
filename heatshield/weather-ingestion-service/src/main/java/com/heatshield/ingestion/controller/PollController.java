package com.heatshield.ingestion.controller;

import com.heatshield.ingestion.service.WeatherIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class PollController {

    private final WeatherIngestionService weatherIngestionService;

    @GetMapping("/poll/trigger")
    public Map<String, String> triggerPoll() {
        weatherIngestionService.pollAllCities();
        return Map.of("status", "poll triggered");
    }

    @GetMapping("/poll/haridwar")
    public Map<String, Object> pollHaridwar() {
        var city = new WeatherIngestionService.CityRef(
            "city-haridwar", "Haridwar", "IN", "AS", 29.9457, 78.1642
        );
        try {
            weatherIngestionService.processCity(city);
            return Map.of("status", "success", "city", "Haridwar");
        } catch (Exception e) {
            return Map.of("status", "error", "message", e.getMessage());
        }
    }
}
