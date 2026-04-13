package com.shinhan.esg_be.domain.recommendation.dto;

import com.shinhan.esg_be.domain.user.entity.enums.Grade;
import com.shinhan.esg_be.domain.user.entity.enums.UserType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class UserFeatureDto {

    // 사용자 기본 정보
    private Long     userId;
    private UserType userType;
    private Grade    currentGrade;

    // ESG 점수
    private int eScore;
    private int sScore;
    private int gActivityScore;

    // 다음 등급까지 남은 점수
    private int nextGradeGap;

    // 이번 달 카테고리별 획득 점수
    private int monthlyEScore;
    private int monthlySScore;
    private int monthlyGScore;

    // 최근 90일 카테고리별 활동 횟수
    private int recentECount;
    private int recentSCount;
    private int recentGCount;
    private int totalActivityCount; // 최근 90일 총 활동 횟수 (C1 α 계산용)

    // 가장 활동 비율이 낮은 카테고리
    private String weakestCategory;

    // 오늘 퀴즈 참여 여부
    private boolean todayQuizDone;

    // 소프트 부스트용
    private boolean inactivityRisk;
    private boolean scoreExpiryRisk;

    // 금융 상품 상태
    private boolean   hasSaving;
    private boolean   hasLoan;
    private LocalDate savingMaturityDate;
    private Long      savingFinProductId;
}
