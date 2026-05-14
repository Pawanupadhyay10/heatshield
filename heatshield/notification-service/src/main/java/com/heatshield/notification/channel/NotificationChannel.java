package com.heatshield.notification.channel;

import com.heatshield.notification.model.AlertCommand;
import com.heatshield.notification.model.DeliveryResult;

/**
 * Strategy pattern — each channel implements this interface.
 *
 * INTERVIEW TALKING POINT:
 * "I use the Strategy pattern for notification channels.
 *  Adding a new channel (WhatsApp, Telegram) means implementing
 *  this interface and registering the bean — zero changes to
 *  existing code. Open/Closed Principle in practice."
 */
public interface NotificationChannel {
    String channelName();
    boolean isEnabled();
    DeliveryResult send(AlertCommand command);
}
