package com.heatshield.ingestion.kafka;

import com.heatshield.ingestion.model.HeatEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka Producer — publishes HeatEvents to topic: heat-events
 *
 * Partitioning strategy: key = continentCode
 * This ensures all events for a continent land on the same partition,
 * enabling ordered processing by region in MS-2.
 *
 * INTERVIEW TALKING POINT:
 * "I partition by continent so the heat-processing-service can
 *  scale consumers per continent independently. Asia gets more
 *  partitions than Antarctica."
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HeatEventProducer {

    private static final String TOPIC = "heat-events";

    private final KafkaTemplate<String, HeatEvent> kafkaTemplate;

    public void publish(HeatEvent event) {
        // Partition key = continentCode (AS, EU, AF, NA, SA, OC)
        CompletableFuture<SendResult<String, HeatEvent>> future =
                kafkaTemplate.send(TOPIC, event.getContinentCode(), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish HeatEvent for city={}: {}",
                        event.getCityName(), ex.getMessage());
                // TODO Week 6: publish to DLQ topic for retry
            } else {
                log.debug("Published to partition={} offset={}",
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
