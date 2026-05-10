package com.heatshield.agent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertCommand {
    private String alertId;
    private String cityId;
    private String cityName;
    private String countryCode;
    private double lat;
    private double lng;
    private String severity;
    private String message;
    private double heatIndex;
    private double vulnerabilityScore;
    private long   estimatedAtRisk;
    private String coolShelterInfo;
    private long   timestamp;
    private String agentRunId;
}
