package com.heatshield.routing.repository;

import com.heatshield.routing.entity.CoolShelterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface CoolShelterRepository
        extends JpaRepository<CoolShelterEntity, UUID> {

    /**
     * Core spatial query — finds shelters within radius using PostGIS.
     *
     * ST_DWithin uses the GiST spatial index — orders of magnitude
     * faster than ST_Distance for proximity queries.
     *
     * ST_Distance returns degrees (EPSG:4326) — multiply by 111.32
     * to convert to kilometres.
     *
     * INTERVIEW TALKING POINT:
     * "ST_DWithin with a GiST index runs in O(log n) time.
     *  A naive distance calculation would be O(n).
     *  For 1M shelters globally, that's the difference between
     *  8ms and 8 seconds per request."
     */
    @Query(value = """
        SELECT *,
               ST_Distance(location,
                   ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)) * 111320 AS dist_metres
        FROM cool_shelters
        WHERE ST_DWithin(
            location,
            ST_SetSRID(ST_MakePoint(:lng, :lat), 4326),
            :radiusDegrees
        )
        ORDER BY dist_metres ASC
        LIMIT :maxResults
        """, nativeQuery = true)
    List<CoolShelterEntity> findNearbyShelters(
            @Param("lat")          double lat,
            @Param("lng")          double lng,
            @Param("radiusDegrees") double radiusDegrees,
            @Param("maxResults")   int    maxResults
    );

    @Query(value = """
        SELECT COUNT(*) FROM cool_shelters
        WHERE country_code = :countryCode
        """, nativeQuery = true)
    long countByCountryCode(@Param("countryCode") String countryCode);

    @Query(value = "SELECT COUNT(*) FROM cool_shelters", nativeQuery = true)
    long countAllShelters();
}
