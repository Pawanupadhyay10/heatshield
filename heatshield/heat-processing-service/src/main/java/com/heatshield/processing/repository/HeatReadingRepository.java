package com.heatshield.processing.repository;

import com.heatshield.processing.entity.HeatReadingEntity;
import com.heatshield.processing.entity.HeatReadingId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface HeatReadingRepository
        extends JpaRepository<HeatReadingEntity, HeatReadingId> {

    @Query(value = """
        SELECT * FROM heat_readings
        WHERE city_text_id = :cityId
        ORDER BY time DESC
        LIMIT 1
        """, nativeQuery = true)
    Optional<HeatReadingEntity> findLatestByCityId(@Param("cityId") String cityId);

    @Query(value = """
        SELECT DISTINCT ON (city_text_id) *
        FROM heat_readings
        WHERE risk_tier IN ('DANGER', 'EXTREME')
          AND time >= NOW() - INTERVAL '30 minutes'
        ORDER BY city_text_id, time DESC
        """, nativeQuery = true)
    List<HeatReadingEntity> findCurrentHighRiskCities();

    @Query(value = """
        SELECT COUNT(*) FROM heat_readings
        WHERE time >= NOW() - INTERVAL '24 hours'
        """, nativeQuery = true)
    long countLast24Hours();
}
