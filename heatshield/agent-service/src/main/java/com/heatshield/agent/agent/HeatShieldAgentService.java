package com.heatshield.agent.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface HeatShieldAgentService {

    @SystemMessage("""
        You are HeatShield, an AI agent protecting people from dangerous heat.

        Your mission: analyse heat risk data and decide whether to dispatch alerts.

        You have 5 tools:
        1. fetchHeatIndex — get current heat conditions
        2. queryVulnerablePopulation — assess population at risk
        3. predictHeatWindow — forecast dangerous heat windows
        4. findCoolCorridors — locate nearest cool shelters
        5. dispatchAlert — send emergency alert (use carefully)

        Rules:
        - ONLY dispatch if heatIndex > 41 AND vulnerabilityScore > 50
        - ALWAYS fetch heat index and population BEFORE deciding
        - ALWAYS find cool corridors to include shelter info in message
        - Write alerts in clear, urgent but calm language
        - If conditions are mild or improving, do NOT dispatch
        """)
    @UserMessage("""
        Risk event triggered for {{cityName}} ({{cityId}}).
        Current tier: {{currentTier}} (from {{previousTier}}).
        Heat Index: {{heatIndex}}C. At risk: {{estimatedAtRisk}} people.
        Location: lat={{lat}}, lng={{lng}}. Country: {{countryCode}}.
        Assess using your tools and decide whether to dispatch an alert.
        """)
    String assessAndAct(
            @V("cityId")          String cityId,
            @V("cityName")        String cityName,
            @V("currentTier")     String currentTier,
            @V("previousTier")    String previousTier,
            @V("heatIndex")       double heatIndex,
            @V("estimatedAtRisk") long   estimatedAtRisk,
            @V("lat")             double lat,
            @V("lng")             double lng,
            @V("countryCode")     String countryCode
    );
}
