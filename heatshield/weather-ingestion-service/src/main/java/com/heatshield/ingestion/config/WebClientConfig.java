package com.heatshield.ingestion.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${owm.base-url:https://api.openweathermap.org}")
    private String owmBaseUrl;

    @Bean
    public WebClient owmWebClient() {
        return WebClient.builder()
                .baseUrl(owmBaseUrl)
                .defaultHeader("Accept", "application/json")
                .codecs(c -> c.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
    }
}
