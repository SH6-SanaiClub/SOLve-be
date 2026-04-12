package com.shinhan.esg_be.domain.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityRecommendResponse {

    private List<RecommendedActivity> activities;  // 추천 활동 Top 3
    private RecommendedActivity popularActivity;    // 인기 추천 1개
    private String llmSummary;                      // LLM 자연어 설명

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
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
        private String mainReason;     // 추천사유

        // UI 표시용
        private Long currentAmount;    // 기부 현재 모금액
        private Long targetAmount;     // 기부 목표 모금액
        private Integer currentEnrolled; // 봉사 현재 신청 인원
        private Integer capacity;       // 봉사 정원

        // LLM 생성 카드 멘트
        private String description;

        // 인기 활동 전용 상태 필드
        private Boolean alreadyParticipatedToday; // 오늘 이미 참여했는지 (E/G)
        private Boolean monthlyLimitReached;      // 월 점수 한도 도달 여부
    }
}
