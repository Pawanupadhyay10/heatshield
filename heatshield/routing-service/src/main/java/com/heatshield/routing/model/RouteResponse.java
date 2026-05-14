package com.heatshield.routing.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteResponse {
    private double originLat;
    private double originLng;
    private String currentRiskTier;
    private double currentHeatIndex;
    private List<ShelterResult> shelters;   // sorted by exposure score
    private String generalAdvice;
    private long   cachedAt;
    private String source;                  // postgis_live | redis_cache | fallback
}
