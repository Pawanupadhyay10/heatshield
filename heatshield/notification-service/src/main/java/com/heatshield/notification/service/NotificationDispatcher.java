package com.heatshield.notification.service;

import com.heatshield.notification.channel.NotificationChannel;
import com.heatshield.notification.entity.NotificationLogEntity;
import com.heatshield.notification.model.AlertCommand;
import com.heatshield.notification.model.DeliveryResult;
import com.heatshield.notification.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.List;

/**
 * Dispatches alerts across all enabled channels.
 *
 * INTERVIEW TALKING POINT:
 * "The dispatcher iterates all registered channels via Spring's
 *  dependency injection — List<NotificationChannel> auto-wires
 *  all beans implementing the interface. Adding a new channel
 *  requires zero changes here. This is the Open/Closed Principle."
 *
 * Retry logic:
 *   - Each channel gets 3 attempts with exponential backoff
 *   - If all 3 fail, message goes to DLQ (agent-dlq topic)
 *   - Redis channel always succeeds — guaranteed minimum delivery
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDispatcher {

    // Spring auto-wires ALL beans implementing NotificationChannel
    private final List<NotificationChannel>  channels;
    private final NotificationLogRepository  logRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final KafkaTemplate<String, AlertCommand> kafkaTemplate;

    private static final int    MAX_RETRIES     = 3;
    private static final String DLQ_TOPIC       = "agent-dlq";
    private static final Duration DEDUP_WINDOW  = Duration.ofMinutes(60);

    public void dispatch(AlertCommand command) {
        log.info("Dispatching alert: city={} severity={} channels={}",
                command.getCityName(), command.getSeverity(), channels.size());

        // Dedup check — prevent duplicate alerts per city per hour
        String dedupKey = "notif:sent:" + command.getCityId()
                + ":" + command.getSeverity();
        if (Boolean.TRUE.equals(redisTemplate.hasKey(dedupKey))) {
            log.info("Alert suppressed — dedup active: city={}", command.getCityId());
            return;
        }

        int successCount = 0;
        int failCount    = 0;

        for (NotificationChannel channel : channels) {
            if (!channel.isEnabled()) {
                log.debug("Channel {} disabled — skipping", channel.channelName());
                continue;
            }

            DeliveryResult result = sendWithRetry(channel, command);

            // Log every attempt
            saveLog(command, result, 1, false);

            if (result.isSuccess()) {
                successCount++;
                log.info("✓ Channel {} succeeded for city={}",
                        channel.channelName(), command.getCityName());
            } else {
                failCount++;
                log.warn("✗ Channel {} failed for city={}: {}",
                        channel.channelName(), command.getCityName(),
                        result.getErrorMessage());
            }
        }

        // Set dedup key after successful dispatch
        if (successCount > 0) {
            redisTemplate.opsForValue().set(dedupKey, "sent", DEDUP_WINDOW);
        }

        // Send to DLQ if ALL channels failed
        if (successCount == 0 && failCount > 0) {
            log.error("ALL channels failed for city={} — sending to DLQ",
                    command.getCityName());
            sendToDlq(command);
        }

        log.info("Dispatch complete: city={} success={} failed={}",
                command.getCityName(), successCount, failCount);
    }

    /**
     * Retry with exponential backoff.
     * Attempts: 1 (immediate) → 2 (2s delay) → 3 (4s delay)
     */
    private DeliveryResult sendWithRetry(
            NotificationChannel channel, AlertCommand command) {
        DeliveryResult result = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                result = channel.send(command);
                if (result.isSuccess()) return result;

                if (attempt < MAX_RETRIES) {
                    long delay = (long) Math.pow(2, attempt) * 1000;
                    log.warn("Channel {} attempt {}/{} failed — retrying in {}ms",
                            channel.channelName(), attempt, MAX_RETRIES, delay);
                    Thread.sleep(delay);
                }
            } catch (Exception e) {
                log.error("Channel {} attempt {} threw exception: {}",
                        channel.channelName(), attempt, e.getMessage());
                result = DeliveryResult.builder()
                        .channel(channel.channelName())
                        .success(false)
                        .errorMessage(e.getMessage())
                        .build();
            }
        }
        return result;
    }

    private void sendToDlq(AlertCommand command) {
        try {
            kafkaTemplate.send(DLQ_TOPIC, command.getCityId(), command);
            log.warn("Sent to DLQ: city={}", command.getCityName());
        } catch (Exception e) {
            log.error("Failed to send to DLQ: {}", e.getMessage());
        }
    }

    private void saveLog(AlertCommand command, DeliveryResult result,
                          int attempt, boolean sentToDlq) {
        try {
            logRepository.save(NotificationLogEntity.builder()
                    .alertId(command.getAlertId())
                    .cityId(command.getCityId())
                    .cityName(command.getCityName())
                    .severity(command.getSeverity())
                    .channel(result.getChannel())
                    .success(result.isSuccess())
                    .recipientId(result.getRecipientId())
                    .messageId(result.getMessageId())
                    .errorMessage(result.getErrorMessage())
                    .attemptNumber(attempt)
                    .durationMs(result.getDurationMs())
                    .sentToDlq(sentToDlq)
                    .build());
        } catch (Exception e) {
            log.error("Failed to save notification log: {}", e.getMessage());
        }
    }
}
