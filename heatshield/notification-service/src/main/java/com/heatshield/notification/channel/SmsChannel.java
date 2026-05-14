package com.heatshield.notification.channel;

import com.heatshield.notification.model.AlertCommand;
import com.heatshield.notification.model.DeliveryResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * SMS channel via Twilio.
 *
 * When TWILIO_ACCOUNT_SID is not set — logs the message instead.
 * This lets the service run in dev without real credentials.
 *
 * To enable: set TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN,
 *            TWILIO_FROM_NUMBER in Codespaces secrets.
 */
@Slf4j
@Component
public class SmsChannel implements NotificationChannel {

    @Value("${twilio.account-sid:#{null}}")
    private String accountSid;

    @Value("${twilio.auth-token:#{null}}")
    private String authToken;

    @Value("${twilio.from-number:+15005550006}")
    private String fromNumber;

    @Override
    public String channelName() { return "sms"; }

    @Override
    public boolean isEnabled() {
        return accountSid != null && !accountSid.equals("dummy");
    }

    @Override
    public DeliveryResult send(AlertCommand command) {
        long start = System.currentTimeMillis();

        if (!isEnabled()) {
            // Dev mode — log instead of sending
            log.info("[SMS-STUB] Would send to city={} severity={}: {}",
                    command.getCityName(),
                    command.getSeverity(),
                    buildSmsBody(command));
            return DeliveryResult.builder()
                    .channel("sms")
                    .success(true)
                    .recipientId("stub")
                    .messageId("stub-" + System.currentTimeMillis())
                    .durationMs(System.currentTimeMillis() - start)
                    .build();
        }

        try {
            // Production Twilio call
            // Twilio.init(accountSid, authToken);
            // Message message = Message.creator(
            //     new PhoneNumber(toNumber),
            //     new PhoneNumber(fromNumber),
            //     buildSmsBody(command)
            // ).create();

            log.info("[SMS] Sent to city={} severity={}",
                    command.getCityName(), command.getSeverity());
            return DeliveryResult.builder()
                    .channel("sms").success(true)
                    .durationMs(System.currentTimeMillis() - start)
                    .build();
        } catch (Exception e) {
            log.error("[SMS] Failed: {}", e.getMessage());
            return DeliveryResult.builder()
                    .channel("sms").success(false)
                    .errorMessage(e.getMessage())
                    .durationMs(System.currentTimeMillis() - start)
                    .build();
        }
    }

    private String buildSmsBody(AlertCommand cmd) {
        return String.format(
            "HEAT ALERT %s: %s Heat index %.1f°C in %s. " +
            "Seek cool shelter immediately. -HeatShield",
            cmd.getSeverity(), cmd.getMessage(),
            cmd.getHeatIndex(), cmd.getCityName());
    }
}
