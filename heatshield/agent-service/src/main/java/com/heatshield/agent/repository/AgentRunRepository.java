package com.heatshield.agent.repository;

import com.heatshield.agent.entity.AgentRunEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface AgentRunRepository extends JpaRepository<AgentRunEntity, UUID> {

    @Query(value = """
        SELECT * FROM agent_runs
        WHERE city_id = :cityId
        ORDER BY created_at DESC
        LIMIT 10
        """, nativeQuery = true)
    List<AgentRunEntity> findRecentByCityId(String cityId);

    @Query(value = """
        SELECT COUNT(*) FROM agent_runs
        WHERE alert_dispatched = true
          AND created_at >= NOW() - INTERVAL '24 hours'
        """, nativeQuery = true)
    long countAlertsDispatchedLast24Hours();

    @Query(value = """
        SELECT COUNT(*) FROM agent_runs
        WHERE fallback_used = true
          AND created_at >= NOW() - INTERVAL '1 hour'
        """, nativeQuery = true)
    long countFallbacksLastHour();
}
