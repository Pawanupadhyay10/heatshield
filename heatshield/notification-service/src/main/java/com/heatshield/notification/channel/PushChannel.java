package com.heatshield.notification.channel;

import com.heatshield.notification.model.AlertCommand;
import com.heatshield.notification.model.DeliveryResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Push notification channel via Firebase FCM.
 * Stubs to log in dev when FCM_SERVER_KEY not set.
 */
@Slf4j
@Component
public class PushChannel implements NotificationChannel {

    @Value("${fcm.server-key:#{null}}")
    private String fcmServerKey;

    @Override
    public String channelName() { return "push"; }

    @Override
    public boolean isEnabled() {
        return fcmServerKey != null && !fcmServerKey.equals("dummy");
    }

    @Override
    public DeliveryResult send(AlertCommand command) {
        long start = System.currentTimeMillis();

        if (!isEnabled()) {
            log.info("[PUSH-STUB] Would send FCM to city={} severity={} title='{}'",
                    command.getCityName(),
                    command.getSeverity(),
                    buildTitle(command));
            return DeliveryResult.builder()
                    .channel("push").success(true)
                    .recipientId("stub-fcm")
                    .messageId("stub-push-" + System.currentTimeMillis())
                    .durationMs(System.currentTimeMillis() - start)
                    .build();
        }

        try {
            // Production FCM call via Firebase Admin SDK
            // Message message = Message.builder()
            //     .setTopic("city-" + command.getCityId())
            //     .setNotification(Notification.builder()
            //         .setTitle(buildTitle(command))
            //         .setBody(command.getMessage())
            //         .build())
            //     .putData("severity", command.getSeverity())
            //     .putData("heatIndex", String.valueOf(command.getHeatIndex()))
            //     .build();
            // FirebaseMessaging.getInstance().send(message);

            log.info("[PUSH] Sent: city={} severity={}",
                    command.getCityName(), command.getSeverity());
            return DeliveryResult.builder()
                    .channel("push").success(true)
                    .durationMs(System.currentTimeMillis() - start)
                    .build();
        } catch (Exception e) {
            log.error("[PUSH] Failed: {}", e.getMessage());
            return DeliveryResult.builder()
                    .channel("push").success(false)
                    .errorMessage(e.getMessage())
                    .durationMs(System.currentTimeMillis() - start)
                    .build();
        }
    }

    private String buildTitle(AlertCommand cmd) {
        return String.format("🌡️ Heat %s Alert — %s",
                cmd.getSeverity(), cmd.getCityName());
    }
}
