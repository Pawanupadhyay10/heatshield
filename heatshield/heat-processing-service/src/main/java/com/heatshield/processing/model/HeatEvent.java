package com.heatshield.processing.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeatEvent {
    private String cityId;
    private String cityName;
    private String countryCode;
    private String continentCode;
    private double lat;
    private double lng;
    private double tempC;
    private double feelsLikeC;
    private double humidity;
    private double windSpeedMs;
    private double uvIndex;
    private double heatIndex;
    private String riskTier;
    private long   timestamp;
    private String dataSource;
}
