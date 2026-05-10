package com.heatshield.agent.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "agent_runs")
public class AgentRunEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "triggered_by", nullable = false)
    private String triggeredBy;

    @Column(name = "city_id")
    private String cityId;

    @Column(name = "city_name")
    private String cityName;

    @Column(name = "heat_index_at_run")
    private Double heatIndexAtRun;

    @Column(name = "vulnerability_score")
    private Double vulnerabilityScore;

    @Column(name = "tools_called", columnDefinition = "TEXT")
    private String toolsCalled;

    @Column(name = "alert_dispatched")
    private Boolean alertDispatched;

    @Column(name = "alert_severity")
    private String alertSeverity;

    @Column(name = "fallback_used")
    private Boolean fallbackUsed;

    @Column(name = "llm_tokens_used")
    private Integer llmTokensUsed;

    @Column(name = "total_duration_ms")
    private Long totalDurationMs;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (alertDispatched == null) alertDispatched = false;
        if (fallbackUsed == null) fallbackUsed = false;
    }
}
