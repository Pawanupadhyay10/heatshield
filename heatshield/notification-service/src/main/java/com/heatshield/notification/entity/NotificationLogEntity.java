package com.heatshield.notification.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

/**
 * Audit log for every notification attempt.
 *
 * INTERVIEW TALKING POINT:
 * "Every notification — success or failure — is logged.
 *  This lets me track delivery rates per channel, per city,
 *  per severity level. In production I'd pipe this to
 *  a metrics dashboard to monitor alert reliability."
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "notification_log")
public class NotificationLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "alert_id")
    private String alertId;

    @Column(name = "city_id")
    private String cityId;

    @Column(name = "city_name")
    private String cityName;

    @Column(name = "severity")
    private String severity;

    @Column(name = "channel")
    private String channel;        // sms|email|push|redis

    @Column(name = "success")
    private Boolean success;

    @Column(name = "recipient_id")
    private String recipientId;

    @Column(name = "message_id")
    private String messageId;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "attempt_number")
    private Integer attemptNumber;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "sent_to_dlq")
    private Boolean sentToDlq;

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (sentToDlq == null) sentToDlq = false;
        if (success == null) success = false;
    }
}
