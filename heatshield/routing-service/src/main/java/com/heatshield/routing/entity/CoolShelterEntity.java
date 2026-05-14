package com.heatshield.routing.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;
import java.time.Instant;
import java.util.UUID;

/**
 * Maps to cool_shelters table in PostGIS.
 *
 * INTERVIEW TALKING POINT:
 * "I use JTS Point geometry type mapped via Hibernate Spatial.
 *  PostGIS stores this as GEOMETRY(POINT, 4326) — global WGS84.
 *  The GiST index on location enables ST_DWithin queries in <10ms
 *  even with millions of shelter records."
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cool_shelters")
public class CoolShelterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "osm_id")
    private Long osmId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "shelter_type", nullable = false)
    private String shelterType;

    // PostGIS GEOMETRY(POINT, 4326) — mapped via Hibernate Spatial
    @Column(name = "location", columnDefinition = "geometry(Point,4326)")
    private Point location;

    @Column(name = "address")
    private String address;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Column(name = "capacity_est")
    private Integer capacityEst;

    @Column(name = "ac_confirmed")
    private Boolean acConfirmed;

    @Column(name = "is_verified")
    private Boolean isVerified;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = Instant.now();
        if (acConfirmed == null) acConfirmed = false;
        if (isVerified == null) isVerified = false;
    }
}
