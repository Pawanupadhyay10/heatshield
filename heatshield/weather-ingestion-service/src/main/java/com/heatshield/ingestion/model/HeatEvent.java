package com.heatshield.ingestion.model;

import lombok.Builder;
import lombok.Data;

/**
 * Kafka event published to topic: heat-events
 * Consumed by heat-processing-service (MS-2)
 */
@Data
@Builder
public class HeatEvent {
    private String cityId;
    private String cityName;
    private String countryCode;
    private String continentCode;
    private double lat;
    private double lng;
    private double tempC;
    private double feelsLikeC;
    private double humidity;       // relative humidity %
    private double windSpeedMs;
    private double uvIndex;
    private double heatIndex;      // computed by HeatIndexCalculator
    private String riskTier;       // SAFE | WATCH | WARNING | DANGER | EXTREME
    private long   timestamp;      // epoch millis
    private String dataSource;
}
