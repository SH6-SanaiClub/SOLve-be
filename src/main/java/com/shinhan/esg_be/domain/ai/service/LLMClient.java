package com.shinhan.esg_be.domain.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Service
public class LLMClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${openai.model}")
    private String model;

    public LLMClient(
            @Qualifier("openAiRestClient") RestClient restClient,
            ObjectMapper objectMapper
    ) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
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
                    "temperature", 0.4
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

    public void stream(
            List<Map<String, String>> messages,
            Consumer<String> onChunk,
            Runnable onComplete
    ) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("stream", true);
        body.put("max_tokens", 1000);
        body.put("temperature", 0.7);

        restClient.post()
                .uri("/v1/chat/completions")
                .body(body)
                .exchange((request, response) -> {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {

                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (!line.startsWith("data: ")) {
                                continue;
                            }

                            String data = line.substring(6).trim();
                            if ("[DONE]".equals(data)) {
                                break;
                            }

                            String token = parseStreamToken(data);
                            if (token != null && !token.isEmpty()) {
                                onChunk.accept(token);
                            }
                        }
                        onComplete.run();
                    } catch (IOException e) {
                        log.error("SSE 스트림 읽기 실패", e);
                        throw new RuntimeException(e);
                    }
                    return null;
                });
    }

    private String parseStreamToken(String data) {
        try {
            Map<?, ?> map = objectMapper.readValue(data, Map.class);
            List<?> choices = (List<?>) map.get("choices");
            if (choices == null || choices.isEmpty()) {
                return null;
            }
            Map<?, ?> delta = (Map<?, ?>) ((Map<?, ?>) choices.get(0)).get("delta");
            if (delta == null) {
                return null;
            }
            return (String) delta.get("content");
        } catch (Exception e) {
            log.debug("스트림 토큰 파싱 스킵: {}", data);
            return null;
        }
    }
}
