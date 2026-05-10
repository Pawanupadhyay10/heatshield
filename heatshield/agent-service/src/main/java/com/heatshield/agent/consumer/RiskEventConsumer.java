package com.heatshield.agent.consumer;

import com.heatshield.agent.model.RiskEvent;
import com.heatshield.agent.service.AgentOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RiskEventConsumer {

    private final AgentOrchestrator orchestrator;

    @KafkaListener(
            topics           = "risk-alerts",
            groupId          = "agent-service-group",
            containerFactory = "agentKafkaListenerContainerFactory",
            concurrency      = "1"
    )
    public void consume(
            @Payload RiskEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment ack) {
        log.info("RiskEvent: partition={} offset={} city={} tier={}→{}",
                partition, offset, event.getCityName(),
                event.getPreviousTier(), event.getCurrentTier());
        try {
            orchestrator.processRiskEvent(event);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed RiskEvent city={}: {}", event.getCityName(), e.getMessage());
        }
    }
}
