package com.heatshield.processing.kafka;

import com.heatshield.processing.model.RiskEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RiskEventProducer {

    private static final String TOPIC = "risk-alerts";
    private final KafkaTemplate<String, RiskEvent> riskKafkaTemplate;

    public void publish(RiskEvent event) {
        riskKafkaTemplate.send(TOPIC, event.getContinentCode(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish RiskEvent city={}: {}",
                                event.getCityName(), ex.getMessage());
                    } else {
                        log.info("RiskEvent published: city={} tier={}→{}",
                                event.getCityName(),
                                event.getPreviousTier(),
                                event.getCurrentTier());
                    }
                });
    }
}
