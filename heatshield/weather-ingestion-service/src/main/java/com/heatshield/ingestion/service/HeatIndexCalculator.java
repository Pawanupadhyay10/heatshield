package com.heatshield.ingestion.service;

import org.springframework.stereotype.Component;

@Component
public class HeatIndexCalculator {

    public double calculate(double tempC, double humidity) {
        double T = celsiusToFahrenheit(tempC);
        double R = humidity;

        // For mild conditions use simple formula
        if (T < 80) {
            double hi = 0.5 * (T + 61.0 + (T - 68.0) * 1.2 + R * 0.094);
            return fahrenheitToCelsius(hi);
        }

        // For dry heat (humidity < 40%) Rothfusz is unreliable
        // Use feels-like approximation instead
        if (R < 40) {
            double hi = T - (0.55 - 0.0055 * R) * (T - 58.0);
            return fahrenheitToCelsius(hi);
        }

        // Steadman simplified formula — more stable than Rothfusz
        // Valid for T >= 80°F, R >= 40%
        // Source: Steadman 1979, simplified by Anderson 2013
        double hi = -42.379
                + 2.04901523  * T
                + 10.14333127 * R
                - 0.22475541  * T * R
                - 0.00683783  * T * T
                - 0.05481717  * R * R
                + 0.00122874  * T * T * R
                + 0.00085282  * T * R * R
                - 0.00000199  * T * T * R * R;

        // NWS low-humidity adjustment
        if (R < 13 && T >= 80 && T <= 112) {
            hi -= ((13 - R) / 4.0)
                    * Math.sqrt((17 - Math.abs(T - 95.0)) / 17.0);
        }
        // NWS high-humidity adjustment
        if (R > 85 && T >= 80 && T <= 87) {
            hi += ((R - 85) / 10.0) * ((87 - T) / 5.0);
        }

        return fahrenheitToCelsius(hi);
    }

    public String classifyRisk(double heatIndexC) {
        if (heatIndexC < 27)      return "SAFE";
        else if (heatIndexC < 32) return "WATCH";
        else if (heatIndexC < 41) return "WARNING";
        else if (heatIndexC < 54) return "DANGER";
        else                      return "EXTREME";
    }

    private double celsiusToFahrenheit(double c) { return (c * 9.0 / 5.0) + 32; }
    private double fahrenheitToCelsius(double f) { return (f - 32) * 5.0 / 9.0; }
}
