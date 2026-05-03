package com.heatshield.processing.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "heat_readings")
@IdClass(HeatReadingId.class)
public class HeatReadingEntity {

    @Id
    @Column(name = "time", nullable = false)
    private Instant time;

    @Id
    @Column(name = "city_text_id", nullable = false)
    private String cityId;

    @Column(name = "city_name", nullable = false)
    private String cityName;

    @Column(name = "country_code")
    private String countryCode;

    @Column(name = "continent_code")
    private String continentCode;

    @Column(name = "lat")
    private double lat;

    @Column(name = "lng")
    private double lng;

    @Column(name = "temp_c", nullable = false)
    private double tempC;

    @Column(name = "feels_like_c")
    private double feelsLikeC;

    @Column(name = "humidity", nullable = false)
    private double humidity;

    @Column(name = "wind_speed_ms")
    private double windSpeedMs;

    @Column(name = "uv_index")
    private double uvIndex;

    @Column(name = "heat_index", nullable = false)
    private double heatIndex;

    @Column(name = "risk_tier", nullable = false)
    private String riskTier;

    @Column(name = "trend")
    private String trend;

    @Column(name = "data_source")
    private String dataSource;
}
