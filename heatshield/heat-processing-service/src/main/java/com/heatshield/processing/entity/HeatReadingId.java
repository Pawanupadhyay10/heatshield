package com.heatshield.processing.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HeatReadingId implements Serializable {
    private Instant time;
    private String  cityId;
}
