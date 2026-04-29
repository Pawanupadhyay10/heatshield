package com.heatshield.ingestion.service;

import org.springframework.stereotype.Component;

/**
 * Implements the Steadman Heat Index formula.
 *
 * WHY THIS MATTERS FOR INTERVIEWS:
 * You implemented this yourself — don't use a library.
 * Being able to explain this formula, its accuracy bounds,
 * and why it's preferred over simple temperature shows
 * domain depth that impresses Google interviewers.
 *
 * Accuracy: valid when HI > 27°C and humidity > 40%.
 * For lower values, falls back to simpler Rothfusz method.
 *
 * Reference: Rothfusz (1990) NWS Technical Attachment SR90-23
 */
@Component
public class HeatIndexCalculator {

    // Rothfusz regression coefficients
    private static final double C1 = -8.78469475556;
    private static final double C2 =  1.61139411;
    private static final double C3 =  2.33854883889;
    private static final double C4 = -0.14611605;
    private static final double C5 = -0.012308094;
    private static final double C6 = -0.0164248277778;
    private static final double C7 =  0.002211732;
    private static final double C8 =  0.00072546;
    private static final double C9 = -0.000003582;

    /**
     * Compute Heat Index from temperature (°C) and relative humidity (%).
     *
     * @param tempC      dry-bulb temperature in Celsius
     * @param humidity   relative humidity 0–100
     * @return heat index in Celsius
     */
    public double calculate(double tempC, double humidity) {
        // Convert to Fahrenheit for Rothfusz formula
        double T = celsiusToFahrenheit(tempC);
        double R = humidity;

        double heatIndex;

        // Simple formula for mild conditions
        if (T < 80) {
            heatIndex = 0.5 * (T + 61.0 + (T - 68.0) * 1.2 + R * 0.094);
        } else {
            // Full Rothfusz regression
            heatIndex = C1
                    + C2 * T
                    + C3 * R
                    + C4 * T * R
                    + C5 * T * T
                    + C6 * R * R
                    + C7 * T * T * R
                    + C8 * T * R * R
                    + C9 * T * T * R * R;

            // Adjustments for edge cases (per NWS specification)
            if (R < 13 && T >= 80 && T <= 112) {
                heatIndex -= ((13 - R) / 4.0) * Math.sqrt((17 - Math.abs(T - 95.0)) / 17.0);
            } else if (R > 85 && T >= 80 && T <= 87) {
                heatIndex += ((R - 85) / 10.0) * ((87 - T) / 5.0);
            }
        }

        return fahrenheitToCelsius(heatIndex);
    }

    /**
     * Classify heat index into 5-tier WHO-aligned risk tiers.
     *
     * Thresholds calibrated for global use (not just US):
     * - SAFE:    HI < 27°C  — no significant risk
     * - WATCH:   27–32°C    — fatigue possible with prolonged exposure
     * - WARNING: 32–41°C    — heat cramps/exhaustion possible
     * - DANGER:  41–54°C    — heat stroke highly likely
     * - EXTREME: > 54°C     — life-threatening, emergency response needed
     */
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
