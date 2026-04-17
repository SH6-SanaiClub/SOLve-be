package com.shinhan.esg_be.domain.environment.service.precheck;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shinhan.esg_be.domain.environment.entity.enums.EnvironmentActivityType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class OpenAiImagePrecheckService {

    private static final String DEFAULT_MEDIA_TYPE = "application/octet-stream";
    private static final String SCHEMA_NAME = "environment_image_precheck";

    private final RestClient openAiRestClient;
    private final ObjectMapper objectMapper;

    @Value("${openai.image-precheck-model:${openai.model}}")
    private String model;

    public OpenAiImagePrecheckService(
            @Qualifier("openAiRestClient") RestClient openAiRestClient,
            ObjectMapper objectMapper
    ) {
        this.openAiRestClient = openAiRestClient;
        this.objectMapper = objectMapper;
    }

    public ImagePrecheckResult precheck(MultipartFile image, EnvironmentActivityType activityType) {
        try {
            String responseContent = requestPrecheck(toDataUrl(image), activityType);
            ImagePrecheckResult result = parseResult(responseContent);

            log.info(
                    "OpenAI image precheck completed: activityType={}, allowed={}, reason={}",
                    activityType,
                    result.allowed(),
                    result.reason()
            );
            return result;
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "OpenAI 이미지 사전검사 중 파일을 읽지 못했습니다.",
                    exception
            );
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "OpenAI 이미지 사전검사에 실패했습니다.",
                    exception
            );
        }
    }

    private String requestPrecheck(String dataUrl, EnvironmentActivityType activityType) {
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of(
                                "role", "system",
                                "content", buildSystemPrompt()
                        ),
                        Map.of(
                                "role", "user",
                                "content", List.of(
                                        Map.of(
                                                "type", "text",
                                                "text", buildUserPrompt(activityType)
                                        ),
                                        Map.of(
                                                "type", "image_url",
                                                "image_url", Map.of(
                                                        "url", dataUrl,
                                                        "detail", "low"
                                                )
                                        )
                                )
                        )
                ),
                "response_format", Map.of(
                        "type", "json_schema",
                        "json_schema", Map.of(
                                "name", SCHEMA_NAME,
                                "strict", true,
                                "schema", buildResponseSchema()
                        )
                ),
                "temperature", 0,
                "max_tokens", 180
        );

        Map<?, ?> response = openAiRestClient.post()
                .uri("/v1/chat/completions")
                .body(requestBody)
                .retrieve()
                .body(Map.class);

        return extractMessageContent(response);
    }

    private String toDataUrl(MultipartFile image) throws IOException {
        String mediaType = image.getContentType();
        if (mediaType == null || mediaType.isBlank()) {
            mediaType = DEFAULT_MEDIA_TYPE;
        }

        String base64 = Base64.getEncoder().encodeToString(image.getBytes());
        return "data:" + mediaType + ";base64," + base64;
    }

    private ImagePrecheckResult parseResult(String content) {
        try {
            JsonNode root = objectMapper.readTree(stripCodeFence(content));

            if (!root.has("allowed")) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "OpenAI 이미지 사전검사 응답에 allowed 필드가 없습니다."
                );
            }

            boolean allowed = root.path("allowed").asBoolean(false);
            String reason = root.path("reason").asText();

            return allowed
                    ? ImagePrecheckResult.allow(reason)
                    : ImagePrecheckResult.reject(reason);
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "OpenAI 이미지 사전검사 응답을 파싱하지 못했습니다.",
                    exception
            );
        }
    }

    private String extractMessageContent(Map<?, ?> response) {
        if (response == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "OpenAI 이미지 사전검사 응답이 비어 있습니다."
            );
        }

        Object choicesValue = response.get("choices");
        if (!(choicesValue instanceof List<?> choices) || choices.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "OpenAI 이미지 사전검사 응답에 choices가 없습니다."
            );
        }

        Object firstChoice = choices.get(0);
        if (!(firstChoice instanceof Map<?, ?> choice)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "OpenAI 이미지 사전검사 응답 형식이 올바르지 않습니다."
            );
        }

        Object messageValue = choice.get("message");
        if (!(messageValue instanceof Map<?, ?> message)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "OpenAI 이미지 사전검사 응답에 message가 없습니다."
            );
        }

        Object contentValue = message.get("content");
        if (contentValue instanceof String content && !content.isBlank()) {
            return content;
        }

        if (contentValue instanceof List<?> contentBlocks) {
            for (Object blockValue : contentBlocks) {
                if (!(blockValue instanceof Map<?, ?> block)) {
                    continue;
                }

                Object textValue = block.get("text");
                if (textValue instanceof String text && !text.isBlank()) {
                    return text;
                }

                if (textValue instanceof Map<?, ?> nestedText) {
                    Object nestedValue = nestedText.get("value");
                    if (nestedValue instanceof String text && !text.isBlank()) {
                        return text;
                    }
                }
            }
        }

        throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "OpenAI 이미지 사전검사 응답에서 content를 찾지 못했습니다."
        );
    }

    private Map<String, Object> buildResponseSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "allowed", Map.of(
                                "type", "boolean",
                                "description", "Whether the image may proceed to OCR."
                        ),
                        "reason", Map.of(
                                "type", "string",
                                "description", "Short reason for the decision."
                        )
                ),
                "required", List.of("allowed", "reason"),
                "additionalProperties", false
        );
    }

    private String buildSystemPrompt() {
        return """
                You review a single uploaded image before OCR for an ESG activity verification flow.
                Decide only whether the image is clearly safe to continue to OCR.
                If you are not confident that the image is normal and trustworthy, reject it.

                Reject when there is even a small sign of manipulation or visual inconsistency, such as:
                - edited, pasted, or replaced text regions
                - UI compositing or mixed interface fragments
                - unnatural numbers, timestamps, prices, amounts, or payment fields
                - inconsistent fonts, spacing, alignment, sharpness, or color within key text blocks
                - duplicated interface fragments, repeated patterns, or cloned areas
                - obvious cut-and-paste edges, seams, overlays, or synthetic artifacts

                Important:
                - If text, amount, date, time, payment, ride summary, rental summary, or discount-related regions look even slightly suspicious, reject the image.
                - If the image looks partially manipulated, reject the image.
                - If the authenticity of the screenshot cannot be judged confidently, reject the image.
                - The reason field must always be written in Korean.

                Do not reject only because the image is:
                - blurry
                - compressed
                - slightly cropped
                - a normal screenshot with common mobile capture noise

                Output valid JSON only.
                Use this schema exactly:
                {
                  "allowed": true,
                  "reason": "짧은 한국어 설명"
                }
                """;
    }

    private String buildUserPrompt(EnvironmentActivityType activityType) {
        return """
                Activity type: %s
                Expected image type: %s

                Determine whether this image should proceed to OCR.
                Return allowed=false if there is any suspicious sign in text regions, UI composition,
                number/date/time/amount/payment fields, or repeated interface patterns.
                If you are not confident that the image is authentic, return allowed=false.
                Write the reason in Korean only.
                """.formatted(activityType.getCode(), describeExpectedImage(activityType));
    }

    private String describeExpectedImage(EnvironmentActivityType activityType) {
        return switch (activityType) {
            case TUMBLER -> "receipt-like or payment-proof screenshot showing a tumbler discount or similar purchase evidence";
            case SHARED_BIKE -> "ride summary or payment screenshot from a shared bike service";
            case EV_RENTAL -> "rental summary or payment screenshot from an electric vehicle rental service";
        };
    }

    private String stripCodeFence(String content) {
        String trimmed = content == null ? "" : content.trim();

        if (!trimmed.startsWith("```")) {
            return trimmed;
        }

        int firstLineBreak = trimmed.indexOf('\n');
        int lastFence = trimmed.lastIndexOf("```");

        if (firstLineBreak < 0 || lastFence <= firstLineBreak) {
            return trimmed;
        }

        return trimmed.substring(firstLineBreak + 1, lastFence).trim();
    }
}
