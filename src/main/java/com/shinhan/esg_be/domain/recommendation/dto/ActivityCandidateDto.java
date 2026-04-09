package com.shinhan.esg_be.domain.recommendation.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Builder
public class ActivityCandidateDto {

    // 활동 식별 정보
    private String activityType;   // PHOTO / DONATION / VOLUNTEER / PURCHASE / QUIZ
    private Long   referenceId;    // 각 테이블의 PK (activity_id, donation_id 등)
    private String name;           // 활동명 (UI 표시용)
    private String scoreCategory;  // E / S / G

    // 정책 정보 (activity_reward_policy에서 조회)
    private int    scoreValue;     // 원본 점수
    private int    pointValue;     // 포인트 (고정값)
    private double pointRate;      // 포인트율 (결제금액 기반)
    private double difficultyIndex; // 난이도·비용 지수 (하드코딩)

    // 마감/재고 정보 (하드 필터용)
    private LocalDate deadlineDate; // 기부 end_date, 봉사 activity_date
    private boolean isActive;

    private Long      currentAmount;    // 기부 현재 모금액
    private Long      targetAmount;     // 기부 목표 모금액
    private Integer   currentEnrolled;  // 봉사 현재 신청 인원
    private Integer   capacity;         // 봉사 정원

    // 이력 (필터/스코어러용)
    private int       todayCount; // 오늘 수행 횟수 (하드 필터)
    private int       recent14DayCount;  // 최근 14일 수행 횟수 (피로도 감점)

    // 점수 계산 결과 (Scorer가 채움)
    @Setter private double normalizedScore; // 카테고리 월한도 대비 정규화점수
    @Setter private double finalScore; // 최종 추천점수
    @Setter private double boostValue; // 소프트 부스트 합산값
    @Setter private String mainReason; //LLM 힌트용
}
