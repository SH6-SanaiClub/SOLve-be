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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
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
            String responseContent = requestPrecheck(image, activityType);
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

    private String requestPrecheck(MultipartFile image, EnvironmentActivityType activityType) throws IOException {
        List<Map<String, Object>> userContent = new ArrayList<>();
        userContent.add(Map.of(
                "type", "text",
                "text", buildUserPrompt(activityType)
        ));
        userContent.add(buildImageContent(toDataUrl(image)));
        for (String focusedCropDataUrl : buildFocusedCropDataUrls(image, activityType)) {
            userContent.add(buildImageContent(focusedCropDataUrl));
        }

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of(
                                "role", "system",
                                "content", buildSystemPrompt()
                        ),
                        Map.of(
                                "role", "user",
                                "content", userContent
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

    private Map<String, Object> buildImageContent(String dataUrl) {
        return Map.of(
                "type", "image_url",
                "image_url", Map.of(
                        "url", dataUrl,
                        "detail", "high"
                )
        );
    }

    private String toDataUrl(MultipartFile image) throws IOException {
        String mediaType = image.getContentType();
        if (mediaType == null || mediaType.isBlank()) {
            mediaType = DEFAULT_MEDIA_TYPE;
        }

        String base64 = Base64.getEncoder().encodeToString(image.getBytes());
        return "data:" + mediaType + ";base64," + base64;
    }

    private List<String> buildFocusedCropDataUrls(MultipartFile image, EnvironmentActivityType activityType) throws IOException {
        BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(image.getBytes()));
        if (bufferedImage == null) {
            return List.of();
        }

        List<CropWindow> cropWindows = resolveFocusedCropWindows(bufferedImage.getWidth(), bufferedImage.getHeight(), activityType);
        List<String> cropDataUrls = new ArrayList<>(cropWindows.size());

        for (CropWindow cropWindow : cropWindows) {
            BufferedImage croppedImage = bufferedImage.getSubimage(
                    cropWindow.x(),
                    cropWindow.y(),
                    cropWindow.width(),
                    cropWindow.height()
            );
            cropDataUrls.add(toPngDataUrl(croppedImage));
        }

        return cropDataUrls;
    }

    private List<CropWindow> resolveFocusedCropWindows(int imageWidth, int imageHeight, EnvironmentActivityType activityType) {
        return switch (activityType) {
            case TUMBLER -> List.of(
                    createCropWindow(imageWidth, imageHeight, 0.00, 0.42),
                    createCropWindow(imageWidth, imageHeight, 0.12, 0.24)
            );
            case SHARED_BIKE, EV_RENTAL -> List.of(
                    createCropWindow(imageWidth, imageHeight, 0.00, 0.40),
                    createCropWindow(imageWidth, imageHeight, 0.18, 0.28)
            );
        };
    }

    private CropWindow createCropWindow(int imageWidth, int imageHeight, double startYRatio, double heightRatio) {
        int y = Math.max(0, (int) Math.round(imageHeight * startYRatio));
        int requestedHeight = Math.max(40, (int) Math.round(imageHeight * heightRatio));
        int height = Math.min(requestedHeight, imageHeight - y);
        return new CropWindow(0, y, imageWidth, height);
    }

    private String toPngDataUrl(BufferedImage image) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        String base64 = Base64.getEncoder().encodeToString(outputStream.toByteArray());
        return "data:image/png;base64," + base64;
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
            String dateTimeFieldCheck = root.path("dateTimeFieldCheck").asText("");

            if ("tampered".equalsIgnoreCase(dateTimeFieldCheck)) {
                return ImagePrecheckResult.reject(reason);
            }

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
                        ),
                        "dateTimeFieldCheck", Map.of(
                                "type", "string",
                                "enum", List.of("clean", "tampered", "unclear_or_not_visible"),
                                "description", "Assessment of visible tampering signs in the date/time text region."
                        )
                ),
                "required", List.of("allowed", "reason", "dateTimeFieldCheck"),
                "additionalProperties", false
        );
    }

    private String buildSystemPrompt() {
        return """
                You review a single uploaded image before OCR for an ESG activity verification flow.
                Decide only whether the image shows a clear and strong sign of manipulation before OCR.
                Your job is to reduce false positives for normal user screenshots and photos.
                Reject only when there is a concrete, visible reason to believe the image was edited or fabricated.

                Inspection priority:
                1. Inspect the date/time text region first whenever a date, time, payment time, rental period,
                   order time, or ride completion time is visible.
                2. Only after checking that region, inspect other critical numeric fields such as amount,
                   payment summary, discount, QR identifier, vehicle info, or ride summary.
                3. Base the decision on visual evidence in the image, not on OCR assumptions.

                Reject when there is a clear and specific sign of manipulation or visual inconsistency, such as:
                - edited, pasted, or replaced text regions
                - UI compositing or mixed interface fragments
                - unnatural numbers, timestamps, prices, amounts, or payment fields
                - inconsistent fonts, spacing, alignment, sharpness, or color within key text blocks
                - duplicated interface fragments, repeated patterns, or cloned areas
                - obvious cut-and-paste edges, seams, overlays, or synthetic artifacts
                - some digits or separators inside the date/time region having different baseline, kerning,
                  thickness, antialiasing, blur level, color, or edge sharpness from neighboring digits
                - local patches around the date/time region that suggest pasted numbers, erased text, or redraws

                Important:
                - Reject only when suspicion is strong and visually grounded.
                - Do not reject based on vague uneasiness or low confidence alone.
                - If the image appears consistent overall and there is no clear edit evidence, allow it.
                - If suspiciousness may reasonably come from compression, blur, resize, screenshot scaling, glare, or normal mobile capture artifacts, allow it.
                - If different apps or brands may naturally have different layouts, colors, or typography, do not reject only for that reason.
                - If the date/time region looks visually natural, do not reject only because other text is messy or partially hard to read.
                - If text, amount, date, time, payment, ride summary, rental summary, or discount-related regions are readable and internally consistent, prefer allow.
                - If the date/time region is unclear or absent, inspect the next most critical numeric/payment field,
                  but do not invent tampering.
                - The reason field must always be written in Korean.

                Do not reject only because the image is:
                - blurry
                - compressed
                - slightly cropped
                - a normal screenshot with common mobile capture noise
                - from an unfamiliar but plausible app or service layout
                - using a different font, icon style, spacing, or color theme that can naturally occur in a real app

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
                Critical fields to inspect: %s
                The first image is the original full image.
                Any additional images are enlarged crops around likely date/time and critical numeric regions.
                Use the crop images to inspect the date/time digits more strictly than the rest of the image.

                Determine whether this image should proceed to OCR.
                Highest priority: inspect the visible date/time field itself for manipulation traces.
                Compare each digit and separator in that region with neighboring digits and labels.
                Look for replaced or pasted digits, inconsistent spacing, baseline jumps, different stroke thickness,
                different antialiasing, local blur/sharpness mismatch, or patch-like background edits.
                Return allowed=false only when there is a clear and concrete sign of manipulation
                in text regions, UI composition, number/date/time/amount/payment fields, or repeated interface patterns.
                If the date/time region looks natural, do not reject only because other text looks noisy.
                If the date/time region is absent or too unclear to judge, mark dateTimeFieldCheck as unclear_or_not_visible
                and inspect the next most important numeric or payment field.
                If the image is merely low quality, compressed, unfamiliar in layout, or slightly hard to read,
                but still plausibly authentic, return allowed=true.
                If you cannot point to a strong visual reason for manipulation, return allowed=true.
                Write the reason in Korean only.
                """.formatted(
                activityType.getCode(),
                describeExpectedImage(activityType),
                describeCriticalFields(activityType)
        );
    }

    private String describeExpectedImage(EnvironmentActivityType activityType) {
        return switch (activityType) {
            case TUMBLER -> "receipt-like or payment-proof screenshot showing a tumbler discount or similar purchase evidence";
            case SHARED_BIKE -> "ride summary or payment screenshot from a shared bike service";
            case EV_RENTAL -> "rental summary or payment screenshot from an electric vehicle rental service";
        };
    }

    private String describeCriticalFields(EnvironmentActivityType activityType) {
        return switch (activityType) {
            case TUMBLER -> "transaction date/time line, total amount, discount line, merchant name";
            case SHARED_BIKE -> "payment or completion date/time, ride duration, final amount, provider name, QR identifier";
            case EV_RENTAL -> "rental date/time or rental period, payment amount, vehicle info, pickup and return summary";
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

    private record CropWindow(int x, int y, int width, int height) {
    }
}
