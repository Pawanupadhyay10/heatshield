package com.heatshield.notification.repository;

import com.heatshield.notification.entity.NotificationLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface NotificationLogRepository
        extends JpaRepository<NotificationLogEntity, UUID> {

    @Query(value = """
        SELECT COUNT(*) FROM notification_log
        WHERE success = true
          AND created_at >= NOW() - INTERVAL '24 hours'
        """, nativeQuery = true)
    long countSuccessfulLast24Hours();

    @Query(value = """
        SELECT COUNT(*) FROM notification_log
        WHERE sent_to_dlq = true
          AND created_at >= NOW() - INTERVAL '1 hour'
        """, nativeQuery = true)
    long countDlqLast1Hour();

    @Query(value = """
        SELECT COUNT(*) FROM notification_log
        WHERE city_id = :cityId
          AND created_at >= NOW() - INTERVAL '24 hours'
        """, nativeQuery = true)
    long countByCityLast24Hours(String cityId);
}
