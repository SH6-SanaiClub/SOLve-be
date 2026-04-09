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

@Slf4j
@Service
@RequiredArgsConstructor
public class AIService {

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
            String summary = root.path("summary").asText(null);
            JsonNode descriptions = root.path("descriptions");

            List<RecommendedActivity> result = new ArrayList<>(top3);

            for (JsonNode node : descriptions) {
                int index = node.path("index").asInt();
                String description = node.path("description").asText(null);

                if (index < 1 || index > top3.size() || description == null) {
                    continue;
                }

                RecommendedActivity original = result.get(index - 1);
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
                        .description(description)
                        .build());
            }

            return new LLMResult(result, summary);

        } catch (Exception e) {
            log.error("LLM 응답 파싱 실패 raw={}", raw, e);
            return new LLMResult(top3, null);
        }
    }
}
