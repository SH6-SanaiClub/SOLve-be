package com.shinhan.esg_be.domain.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shinhan.esg_be.domain.recommendation.dto.ActivityRecommendResponse.RecommendedActivity;
import com.shinhan.esg_be.domain.recommendation.dto.UserFeatureDto;
import com.shinhan.esg_be.domain.user.entity.enums.Grade;
import com.shinhan.esg_be.domain.user.entity.enums.UserType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AIServiceTest {

    @InjectMocks
    private AIService aiService;

    @Mock
    private LLMClient llmClient;

    @Mock
    private PromptBuilder promptBuilder;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("반말 추천 멘트가 들어오면 존댓말 fallback 문구로 대체한다")
    void replaceCasualToneWithFallback() {
        List<RecommendedActivity> activities = List.of(
                recommendedActivity("QUIZ", "오늘의 ESG 퀴즈", "G", 10),
                recommendedActivity("PHOTO", "텀블러 인증", "E", 8),
                recommendedActivity("DONATION", "기부 캠페인", "S", 12)
        );
        UserFeatureDto feature = userFeature("E");

        when(promptBuilder.buildSystemPrompt()).thenReturn("system");
        when(promptBuilder.buildUserPrompt(activities, feature)).thenReturn("user");
        when(llmClient.call("system", "user")).thenReturn("""
                {
                  "summary": "오늘은 가볍게 해봐",
                  "descriptions": [
                    {"index": 1, "description": "지금 참여해봐"},
                    {"index": 2, "description": "바로 인증해봐"},
                    {"index": 3, "description": "이번에 한번 해보자"}
                  ]
                }
                """);

        AIService.LLMResult result = aiService.generateDescriptions(activities, feature);

        assertThat(result.summary()).isEqualTo("이번 주는 환경 활동부터 차근히 시작해보세요.");
        assertThat(result.activities())
                .extracting(RecommendedActivity::getDescription)
                .containsExactly(
                        "지금 참여하시고 +10점을 챙겨보세요.",
                        "지금 인증하시고 +8점을 챙겨보세요.",
                        "지금 참여하시고 +12점을 챙겨보세요."
                );
    }

    @Test
    @DisplayName("정중한 추천 멘트는 그대로 유지한다")
    void keepPoliteToneWhenValid() {
        List<RecommendedActivity> activities = List.of(
                recommendedActivity("QUIZ", "오늘의 ESG 퀴즈", "G", 10)
        );
        UserFeatureDto feature = userFeature("G");

        when(promptBuilder.buildSystemPrompt()).thenReturn("system");
        when(promptBuilder.buildUserPrompt(activities, feature)).thenReturn("user");
        when(llmClient.call("system", "user")).thenReturn("""
                {
                  "summary": "이번 주는 거버넌스 활동부터 시작해보세요.",
                  "descriptions": [
                    {"index": 1, "description": "오늘 참여하시고 +10점을 챙겨보세요."}
                  ]
                }
                """);

        AIService.LLMResult result = aiService.generateDescriptions(activities, feature);

        assertThat(result.summary()).isEqualTo("이번 주는 거버넌스 활동부터 시작해보세요.");
        assertThat(result.activities().get(0).getDescription())
                .isEqualTo("오늘 참여하시고 +10점을 챙겨보세요.");
    }

    private UserFeatureDto userFeature(String weakestCategory) {
        return UserFeatureDto.builder()
                .userId(1L)
                .userType(UserType.ALL_ROUNDER)
                .currentGrade(Grade.SEED)
                .weakestCategory(weakestCategory)
                .nextGradeGap(50)
                .todayQuizDone(false)
                .build();
    }

    private RecommendedActivity recommendedActivity(
            String activityType,
            String name,
            String scoreCategory,
            int scoreValue
    ) {
        return RecommendedActivity.builder()
                .activityType(activityType)
                .referenceId(1L)
                .name(name)
                .scoreCategory(scoreCategory)
                .scoreValue(scoreValue)
                .pointValue(0)
                .pointRate(0.0)
                .finalScore(1.0)
                .build();
    }
}
