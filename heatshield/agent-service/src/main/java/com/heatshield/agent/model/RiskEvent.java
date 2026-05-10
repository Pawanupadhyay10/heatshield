package com.heatshield.agent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskEvent {
    private String cityId;
    private String cityName;
    private String countryCode;
    private String continentCode;
    private double lat;
    private double lng;
    private double heatIndex;
    private String previousTier;
    private String currentTier;
    private double vulnerabilityScore;
    private long   estimatedAtRisk;
    private long   timestamp;
    private String triggerReason;
}
