package com.heatshield.notification.consumer;

import com.heatshield.notification.model.AlertCommand;
import com.heatshield.notification.service.NotificationDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for alert-commands topic.
 * Published by MS-3 agent when it decides to dispatch an alert.
 *
 * INTERVIEW TALKING POINT:
 * "I separate the alert decision (MS-3 agent) from alert delivery
 *  (MS-5 notification). This means I can scale notification
 *  independently — add more consumers during heatwaves without
 *  touching the agent logic."
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertCommandConsumer {

    private final NotificationDispatcher dispatcher;

    @KafkaListener(
            topics           = "alert-commands",
            groupId          = "notification-service-group",
            containerFactory = "notifKafkaListenerContainerFactory",
            concurrency      = "2"
    )
    public void consume(
            @Payload AlertCommand command,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment ack) {

        log.info("AlertCommand consumed: partition={} offset={} city={} severity={}",
                partition, offset,
                command.getCityName(), command.getSeverity());

        try {
            dispatcher.dispatch(command);
            ack.acknowledge();
            log.info("AlertCommand processed: city={}", command.getCityName());
        } catch (Exception e) {
            log.error("Failed to process AlertCommand city={}: {}",
                    command.getCityName(), e.getMessage());
            // Don't ack — Kafka will redeliver
        }
    }
}
