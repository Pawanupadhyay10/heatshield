package com.heatshield.routing.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShelterResult {
    private String id;
    private String name;
    private String shelterType;
    private double lat;
    private double lng;
    private double distanceKm;
    private double exposureScore;
    private String address;
    private boolean acConfirmed;
    private String routeAdvice;
    private String source;          // postgis_live | fallback
}
