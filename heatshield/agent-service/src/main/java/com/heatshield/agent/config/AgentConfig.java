package com.heatshield.agent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.heatshield.agent.agent.HeatShieldAgentService;
import com.heatshield.agent.model.RiskEvent;
import com.heatshield.agent.tools.HeatShieldTools;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.web.reactive.function.client.WebClient;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class AgentConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:29092}")
    private String bootstrapServers;

    @Value("${gemini.api-key:${GEMINI_API_KEY:dummy}}")
    private String geminiApiKey;

    @Value("${routing.service.url:http://localhost:8084}")
    private String routingServiceUrl;

    /**
     * Gemini via OpenAI-compatible endpoint.
     * Google exposes Gemini at openai-compatible URL so we
     * reuse langchain4j-open-ai (already in pom) — no new dep needed.
     *
     * INTERVIEW TALKING POINT:
     * "I used Gemini's OpenAI-compatible API so I could swap
     *  LLM providers without changing any business logic — just
     *  a config change. This is the adapter pattern."
     */
    @Bean
    public OpenAiChatModel geminiChatModel() {
        return OpenAiChatModel.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta/openai/")
                .apiKey(geminiApiKey)
                .modelName("gemini-2.0-flash-lite")
                .temperature(0.1)
                .maxTokens(1024)
                .timeout(Duration.ofSeconds(30))
                .maxRetries(2)
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    @Bean
    public HeatShieldAgentService heatShieldAgentService(
            OpenAiChatModel geminiChatModel, HeatShieldTools tools) {
        return AiServices.builder(HeatShieldAgentService.class)
                .chatLanguageModel(geminiChatModel)
                .tools(tools)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }

    @Bean
    public ConsumerFactory<String, RiskEvent> riskEventConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "agent-service-group");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.heatshield.*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE,
                "com.heatshield.agent.model.RiskEvent");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, RiskEvent>
    agentKafkaListenerContainerFactory() {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, RiskEvent>();
        factory.setConsumerFactory(riskEventConsumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        var js = new GenericJackson2JsonRedisSerializer(objectMapper());
        template.setValueSerializer(js);
        template.setHashValueSerializer(js);
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public WebClient routingWebClient() {
        return WebClient.builder().baseUrl(routingServiceUrl)
                .defaultHeader("Accept", "application/json").build();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
