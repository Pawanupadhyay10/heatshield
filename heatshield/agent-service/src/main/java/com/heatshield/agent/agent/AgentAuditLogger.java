package com.heatshield.agent.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AgentAuditLogger {

    public void logRequest(String agentName, String userMessage) {
        log.info("[AUDIT] Agent={} | Request={}", agentName, userMessage);
    }

    public void logResponse(String agentName, String response) {
        log.info("[AUDIT] Agent={} | Response={}", agentName, response);
    }

    public void logError(String agentName, String error) {
        log.error("[AUDIT] Agent={} | Error={}", agentName, error);
    }
}
