package com.heatshield.notification.model;

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
    private String severity;         // WATCH|WARNING|DANGER|EXTREME
    private String message;
    private double heatIndex;
    private double vulnerabilityScore;
    private long   estimatedAtRisk;
    private String coolShelterInfo;
    private long   timestamp;
    private String agentRunId;
}
