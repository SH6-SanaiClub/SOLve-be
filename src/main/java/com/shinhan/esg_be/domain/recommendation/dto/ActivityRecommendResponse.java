package com.shinhan.esg_be.domain.recommendation.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class ActivityRecommendResponse {

    private List<RecommendedActivity> activities;  // 추천 활동 Top 3
    private RecommendedActivity popularActivity;    // 인기 추천 1개
    private String llmSummary;                      // LLM 자연어 설명

    @Getter
    @Builder
    public static class RecommendedActivity {
        private String activityType;   // PHOTO / DONATION / VOLUNTEER / PURCHASE / QUIZ
        private Long referenceId;      // 각 테이블 PK
        private String name;           // 활동명
        private String scoreCategory;  // E / S / G
        private int scoreValue;        // 획득 점수
        private int pointValue;        // 획득 포인트 (고정값)
        private double pointRate;      // 포인트율 (결제 기반)
        private LocalDate deadlineDate; // 마감일 (기부/봉사)
        private double finalScore;     // 추천 점수 (디버깅/투명성)
    }
}
