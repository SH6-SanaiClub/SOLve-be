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
    private Long userId;
    private UserType userType;
    private Grade currentGrade;

    // ESG 점수
    private int eScore;
    private int sScore;
    private int gActivityScore;
    private int gRepaymentScore;

    // 이번 달 카테고리별 획득 점수 (월 한도 필터용)
    private int monthlyEScore;
    private int monthlySScore;
    private int monthlyGScore;

    // 최근 90일 카테고리별 활동 횟수 (C1 행동패턴, C2 균형보정용)
    private int recentECount;   // user_activity
    private int recentSCount;   // donation + volunteer(COMPLETED) + eco_product
    private int recentGCount;   // user_quiz

    // 미활동 패널티 위험 여부 (소프트 부스트용)
    private boolean inactivityRisk;  // 최근 30일 활동 없음

    // 유효점수 만료 임박 여부 (소프트 부스트용)
    private boolean scoreExpiryRisk; // valid_until 30일 이내 기록 존재

    // 금융 상품 상태 (B3 금융연계도용)
    private boolean hasSaving;
    private boolean hasLoan;
    private LocalDate savingMaturityDate;
    private Long savingFinProductId;
}