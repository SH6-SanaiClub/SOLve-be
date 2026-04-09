package com.shinhan.esg_be.domain.ai.service;

import com.shinhan.esg_be.domain.recommendation.dto.ActivityRecommendResponse.RecommendedActivity;
import com.shinhan.esg_be.domain.recommendation.dto.UserFeatureDto;
import com.shinhan.esg_be.domain.user.entity.enums.Grade;
import com.shinhan.esg_be.domain.user.entity.enums.UserType;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class PromptBuilder {

    public String buildSystemPrompt() {
        return """
                너는 ESG 금융 플랫폼의 AI 어드바이저야.
                사용자 데이터와 추천 활동 정보를 바탕으로 응답해.
                반드시 아래 JSON 형식으로만 응답해. 마크다운, 코드블록, 설명 텍스트는 절대 포함하지 마.
                {
                  "summary": "전체 요약 한 줄 (40자 이내, 친근한 말투)",
                  "descriptions": [
                    {"index": 1, "description": "카드 멘트 (30자 이내)"},
                    {"index": 2, "description": "카드 멘트 (30자 이내)"},
                    {"index": 3, "description": "카드 멘트 (30자 이내)"}
                  ]
                }
                """;
    }

    public String buildUserPrompt(List<RecommendedActivity> top3, UserFeatureDto feature) {
        String userState = buildUserState(feature);
        String activityLines = buildActivityLines(top3);
        return userState
                + "\n추천 활동:\n" + activityLines
                + "\nsummary는 사용자 상태 기반 전체 동기부여 한 줄,"
                + " 각 description은 추천이유 힌트 기반으로 왜 지금 해야 하는지 작성해줘.";
    }

    private String buildUserState(UserFeatureDto feature) {
        return String.format("""
                사용자 상태:
                - 유형: %s
                - 현재 등급: %s / 다음 등급까지 남은 점수: %d점
                - 가장 부족한 활동 카테고리: %s
                - 오늘 퀴즈 참여: %s
                """,
                userTypeLabel(feature.getUserType()),
                gradeLabel(feature.getCurrentGrade()),
                feature.getNextGradeGap(),
                feature.getWeakestCategory(),
                feature.isTodayQuizDone() ? "완료" : "미완료"
        );
    }

    private String buildActivityLines(List<RecommendedActivity> top3) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < top3.size(); i++) {
            RecommendedActivity a = top3.get(i);

            String point = a.getPointValue() > 0
                    ? " +" + a.getPointValue() + "P" : "";
            String deadline = "";
            if (a.getDeadlineDate() != null) {
                long days = ChronoUnit.DAYS.between(LocalDate.now(), a.getDeadlineDate());
                deadline = " (마감 " + days + "일 이내)";
            }

            sb.append(String.format("%d번: [%s] %s +%d점%s%s | 추천이유: %s\n",
                    i + 1,
                    a.getScoreCategory(),
                    a.getName(),
                    a.getScoreValue(),
                    point,
                    deadline,
                    a.getMainReason() != null ? a.getMainReason() : "종합추천"
            ));
        }
        return sb.toString();
    }

    private String userTypeLabel(UserType type) {
        return switch (type) {
            case GREEN -> "GREEN (그린 실천가)";
            case SOCIAL -> "SOCIAL (소셜 서포터)";
            case FINANCE -> "FINANCE (금융 빌더)";
            case ALL_ROUNDER -> "ALL_ROUNDER (올라운더)";
        };
    }

    private String gradeLabel(Grade grade) {
        return switch (grade) {
            case SEED -> "SEED (씨앗)";
            case SPROUT -> "SPROUT (새싹)";
            case TREE -> "TREE (나무)";
            case FOREST -> "FOREST (숲)";
            case EARTH -> "EARTH (지구)";
        };
    }
}
