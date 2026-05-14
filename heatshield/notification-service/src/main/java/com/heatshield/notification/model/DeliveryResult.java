package com.heatshield.notification.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryResult {
    private String  channel;       // sms|email|push|redis
    private boolean success;
    private String  recipientId;
    private String  messageId;
    private String  errorMessage;
    private long    durationMs;
}
