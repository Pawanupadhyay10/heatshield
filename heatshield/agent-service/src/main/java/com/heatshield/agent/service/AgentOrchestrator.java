package com.heatshield.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heatshield.agent.agent.HeatShieldAgentService;
import com.heatshield.agent.entity.AgentRunEntity;
import com.heatshield.agent.model.RiskEvent;
import com.heatshield.agent.repository.AgentRunRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentOrchestrator {

    private final HeatShieldAgentService agentService;
    private final AgentRunRepository     agentRunRepository;
    private final FallbackAlertService   fallbackAlertService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper           objectMapper;

    private volatile int consecutiveLlmFailures = 0;
    private static final int FALLBACK_THRESHOLD  = 3;

    public void processRiskEvent(RiskEvent event) {
        long start = System.currentTimeMillis();
        log.info("Agent processing: city={} tier={}→{}",
                event.getCityName(), event.getPreviousTier(), event.getCurrentTier());

        AgentRunEntity run = AgentRunEntity.builder()
                .triggeredBy("risk_event")
                .cityId(event.getCityId())
                .cityName(event.getCityName())
                .heatIndexAtRun(event.getHeatIndex())
                .vulnerabilityScore(event.getVulnerabilityScore())
                .alertDispatched(false)
                .fallbackUsed(false)
                .build();

        try {
            if (consecutiveLlmFailures >= FALLBACK_THRESHOLD) {
                log.warn("Circuit open — fallback (failures={})", consecutiveLlmFailures);
                runFallback(event, run);
            } else {
                runLlmAgent(event, run);
            }
        } catch (Exception e) {
            log.error("Orchestrator failed for {}: {}", event.getCityName(), e.getMessage());
            run.setErrorMessage(e.getMessage());
            runFallback(event, run);
        } finally {
            run.setTotalDurationMs(System.currentTimeMillis() - start);
            try {
                agentRunRepository.save(run);
                log.info("Run saved: city={} dispatched={} fallback={} {}ms",
                        event.getCityName(), run.getAlertDispatched(),
                        run.getFallbackUsed(), run.getTotalDurationMs());
            } catch (Exception e) {
                log.error("Failed to save audit log: {}", e.getMessage());
            }
        }
    }

    private void runLlmAgent(RiskEvent event, AgentRunEntity run) {
        try {
            String response = agentService.assessAndAct(
                    event.getCityId(), event.getCityName(),
                    event.getCurrentTier(), event.getPreviousTier(),
                    event.getHeatIndex(), event.getEstimatedAtRisk(),
                    event.getLat(), event.getLng(), event.getCountryCode());
            log.info("Agent response for {}: {}", event.getCityName(), response);
            boolean dispatched = Boolean.TRUE.equals(
                    redisTemplate.hasKey("alert:sent:" + event.getCityId()));
            run.setAlertDispatched(dispatched);
            run.setFallbackUsed(false);
            run.setToolsCalled("[]");
            consecutiveLlmFailures = 0;
        } catch (Exception e) {
            consecutiveLlmFailures++;
            log.error("LLM failed (failures={}): {}", consecutiveLlmFailures, e.getMessage());
            throw e;
        }
    }

    private void runFallback(RiskEvent event, AgentRunEntity run) {
        log.warn("FALLBACK for city={}", event.getCityName());
        run.setFallbackUsed(true);
        boolean dispatched = fallbackAlertService.evaluate(event);
        run.setAlertDispatched(dispatched);
        if (dispatched) run.setAlertSeverity(event.getCurrentTier());
    }
}
