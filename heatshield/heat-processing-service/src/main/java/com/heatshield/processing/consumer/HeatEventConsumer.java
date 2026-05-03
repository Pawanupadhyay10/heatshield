package com.heatshield.processing.consumer;

import com.heatshield.processing.model.HeatEvent;
import com.heatshield.processing.service.HeatProcessingService;
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
public class HeatEventConsumer {

    private final HeatProcessingService processingService;

    @KafkaListener(
            topics           = "heat-events",
            groupId          = "heat-processing-group",
            containerFactory = "kafkaListenerContainerFactory",
            concurrency      = "3"
    )
    public void consume(
            @Payload HeatEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment ack) {

        log.info("Consumed partition={} offset={} city={}",
                partition, offset, event.getCityName());
        try {
            processingService.process(event);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Processing failed city={}: {}", event.getCityName(), e.getMessage());
        }
    }
}
