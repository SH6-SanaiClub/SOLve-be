package com.shinhan.esg_be.domain.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shinhan.esg_be.domain.recommendation.dto.ActivityRecommendResponse.RecommendedActivity;
import com.shinhan.esg_be.domain.recommendation.dto.UserFeatureDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIService {

    private static final int SUMMARY_MAX_LENGTH = 40;
    private static final int DESCRIPTION_MAX_LENGTH = 30;
    private static final Pattern POLITE_ENDING_PATTERN =
            Pattern.compile(".*(요|니다|습니다|세요|까요|드려요|드립니다)$");
    private static final Pattern CASUAL_TONE_PATTERN =
            Pattern.compile(".*(해봐|해라|하자|보자|가자|할래|해도 돼|좋지|챙겨$|해줘$|가봐|써봐).*");

    private final LLMClient llmClient;
    private final PromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;

    public record LLMResult(List<RecommendedActivity> activities, String summary) {
    }

    public LLMResult generateDescriptions(
            List<RecommendedActivity> top3,
            UserFeatureDto feature
    ) {
        String system = promptBuilder.buildSystemPrompt();
        String user = promptBuilder.buildUserPrompt(top3, feature);

        String raw = llmClient.call(system, user);

        if (raw == null) {
            log.warn("LLM 응답 없음 - description 생략");
            return new LLMResult(top3, null);
        }

        try {
            JsonNode root = objectMapper.readTree(raw.trim());
            String summary = sanitizeSummary(root.path("summary").asText(null), feature);
            JsonNode descriptions = root.path("descriptions");

            List<RecommendedActivity> result = new ArrayList<>(top3);

            for (JsonNode node : descriptions) {
                int index = node.path("index").asInt();
                String description = node.path("description").asText(null);

                if (index < 1 || index > top3.size() || description == null) {
                    continue;
                }

                RecommendedActivity original = result.get(index - 1);
                String safeDescription = sanitizeDescription(description, original);
                result.set(index - 1, RecommendedActivity.builder()
                        .activityType(original.getActivityType())
                        .referenceId(original.getReferenceId())
                        .name(original.getName())
                        .scoreCategory(original.getScoreCategory())
                        .scoreValue(original.getScoreValue())
                        .pointValue(original.getPointValue())
                        .pointRate(original.getPointRate())
                        .deadlineDate(original.getDeadlineDate())
                        .finalScore(original.getFinalScore())
                        .mainReason(original.getMainReason())
                        .currentAmount(original.getCurrentAmount())
                        .targetAmount(original.getTargetAmount())
                        .currentEnrolled(original.getCurrentEnrolled())
                        .capacity(original.getCapacity())
                        .description(safeDescription)
                        .alreadyParticipatedToday(original.getAlreadyParticipatedToday())
                        .monthlyLimitReached(original.getMonthlyLimitReached())
                        .canParticipate(original.getCanParticipate())
                        .blockedReasonCode(original.getBlockedReasonCode())
                        .build());
            }

            return new LLMResult(result, summary);

        } catch (Exception e) {
            log.error("LLM 응답 파싱 실패 raw={}", raw, e);
            return new LLMResult(applyFallbackDescriptions(top3), fallbackSummary(feature));
        }
    }

    private List<RecommendedActivity> applyFallbackDescriptions(List<RecommendedActivity> activities) {
        List<RecommendedActivity> result = new ArrayList<>(activities.size());

        for (RecommendedActivity activity : activities) {
            result.add(RecommendedActivity.builder()
                    .activityType(activity.getActivityType())
                    .referenceId(activity.getReferenceId())
                    .name(activity.getName())
                    .scoreCategory(activity.getScoreCategory())
                    .scoreValue(activity.getScoreValue())
                    .pointValue(activity.getPointValue())
                    .pointRate(activity.getPointRate())
                    .deadlineDate(activity.getDeadlineDate())
                    .finalScore(activity.getFinalScore())
                    .mainReason(activity.getMainReason())
                    .currentAmount(activity.getCurrentAmount())
                    .targetAmount(activity.getTargetAmount())
                    .currentEnrolled(activity.getCurrentEnrolled())
                    .capacity(activity.getCapacity())
                    .description(fallbackDescription(activity))
                    .alreadyParticipatedToday(activity.getAlreadyParticipatedToday())
                    .monthlyLimitReached(activity.getMonthlyLimitReached())
                    .canParticipate(activity.getCanParticipate())
                    .blockedReasonCode(activity.getBlockedReasonCode())
                    .build());
        }

        return result;
    }

    private String sanitizeSummary(String summary, UserFeatureDto feature) {
        return sanitizeText(summary, SUMMARY_MAX_LENGTH, fallbackSummary(feature));
    }

    private String sanitizeDescription(String description, RecommendedActivity activity) {
        return sanitizeText(description, DESCRIPTION_MAX_LENGTH, fallbackDescription(activity));
    }

    private String sanitizeText(String rawText, int maxLength, String fallback) {
        String normalized = normalizeText(rawText);

        if (!isFinancialTone(normalized, maxLength)) {
            return fallback;
        }

        return normalized;
    }

    private String normalizeText(String rawText) {
        if (rawText == null) {
            return null;
        }

        return rawText
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replace("\"", "")
                .replace("'", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean isFinancialTone(String text, int maxLength) {
        if (text == null || text.isBlank() || text.length() > maxLength) {
            return false;
        }

        if (text.contains("!") || text.contains("ㅋㅋ") || text.contains("ㅎㅎ")) {
            return false;
        }

        String toneCheckTarget = text.replaceAll("[.!?]+$", "").trim();

        if (CASUAL_TONE_PATTERN.matcher(toneCheckTarget).matches()) {
            return false;
        }

        return POLITE_ENDING_PATTERN.matcher(toneCheckTarget).matches();
    }

    private String fallbackSummary(UserFeatureDto feature) {
        String weakestCategoryLabel = scoreCategoryLabel(feature.getWeakestCategory());
        return "이번 주는 " + weakestCategoryLabel + " 활동부터 차근히 시작해보세요.";
    }

    private String fallbackDescription(RecommendedActivity activity) {
        String actionVerb = switch (activity.getActivityType()) {
            case "PHOTO" -> "인증하시고";
            case "VOLUNTEER" -> "신청하시고";
            default -> "참여하시고";
        };

        return "지금 " + actionVerb + " +" + activity.getScoreValue() + "점을 챙겨보세요.";
    }

    private String scoreCategoryLabel(String scoreCategory) {
        return switch (scoreCategory) {
            case "E" -> "환경";
            case "S" -> "사회";
            case "G" -> "거버넌스";
            default -> "추천";
        };
    }
}
