package com.shinhan.esg_be.domain.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class LLMClient {

    private final RestClient restClient;

    @Value("${openai.model}")
    private String model;

    public LLMClient(@Qualifier("openAiRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public String call(String systemPrompt, String userPrompt) {
        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)
                    ),
                    "max_tokens", 400,
                    "temperature", 0.7
            );

            Map<?, ?> response = restClient.post()
                    .uri("/v1/chat/completions")
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                log.warn("OpenAI 응답이 null");
                return null;
            }

            List<?> choices = (List<?>) response.get("choices");
            Map<?, ?> choice = (Map<?, ?>) choices.get(0);
            Map<?, ?> message = (Map<?, ?>) choice.get("message");
            return (String) message.get("content");

        } catch (Exception e) {
            log.error("OpenAI 호출 실패", e);
            return null;
        }
    }
}
