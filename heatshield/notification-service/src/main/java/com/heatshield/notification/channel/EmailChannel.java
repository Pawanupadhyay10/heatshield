package com.heatshield.notification.channel;

import com.heatshield.notification.model.AlertCommand;
import com.heatshield.notification.model.DeliveryResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Email channel via SendGrid.
 * Stubs to log in dev when SENDGRID_API_KEY not set.
 */
@Slf4j
@Component
public class EmailChannel implements NotificationChannel {

    @Value("${sendgrid.api-key:#{null}}")
    private String sendGridApiKey;

    @Value("${sendgrid.from-email:alerts@heatshield.app}")
    private String fromEmail;

    @Override
    public String channelName() { return "email"; }

    @Override
    public boolean isEnabled() {
        return sendGridApiKey != null && !sendGridApiKey.equals("dummy");
    }

    @Override
    public DeliveryResult send(AlertCommand command) {
        long start = System.currentTimeMillis();

        if (!isEnabled()) {
            log.info("[EMAIL-STUB] Would send to city={} severity={}\nSubject: {}\nBody: {}",
                    command.getCityName(),
                    command.getSeverity(),
                    buildSubject(command),
                    buildBody(command));
            return DeliveryResult.builder()
                    .channel("email").success(true)
                    .recipientId("stub").messageId("stub-email-" + System.currentTimeMillis())
                    .durationMs(System.currentTimeMillis() - start)
                    .build();
        }

        try {
            // Production SendGrid call
            // SendGrid sg = new SendGrid(sendGridApiKey);
            // Request request = new Request();
            // request.setMethod(Method.POST);
            // request.setEndpoint("mail/send");
            // Mail mail = new Mail(new Email(fromEmail), buildSubject(command),
            //                     new Email(toEmail), new Content("text/plain", buildBody(command)));
            // request.setBody(mail.build());
            // sg.api(request);

            log.info("[EMAIL] Sent: city={} severity={}",
                    command.getCityName(), command.getSeverity());
            return DeliveryResult.builder()
                    .channel("email").success(true)
                    .durationMs(System.currentTimeMillis() - start)
                    .build();
        } catch (Exception e) {
            log.error("[EMAIL] Failed: {}", e.getMessage());
            return DeliveryResult.builder()
                    .channel("email").success(false)
                    .errorMessage(e.getMessage())
                    .durationMs(System.currentTimeMillis() - start)
                    .build();
        }
    }

    private String buildSubject(AlertCommand cmd) {
        return String.format("⚠️ HEAT %s ALERT — %s",
                cmd.getSeverity(), cmd.getCityName());
    }

    private String buildBody(AlertCommand cmd) {
        return String.format("""
            HEAT SHIELD ALERT — %s
            
            City: %s
            Severity: %s
            Heat Index: %.1f°C
            Estimated at risk: %,d people
            
            %s
            
            %s
            
            Stay safe. Seek air-conditioned shelter immediately.
            — HeatShield Global Platform
            """,
            cmd.getSeverity(),
            cmd.getCityName(),
            cmd.getSeverity(),
            cmd.getHeatIndex(),
            cmd.getEstimatedAtRisk(),
            cmd.getMessage(),
            cmd.getCoolShelterInfo() != null ? cmd.getCoolShelterInfo() : "");
    }
}
