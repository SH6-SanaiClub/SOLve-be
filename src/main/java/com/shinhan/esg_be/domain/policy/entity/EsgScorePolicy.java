package com.shinhan.esg_be.domain.policy.entity;

import com.shinhan.esg_be.global.common.BaseTimeEntity;
import com.shinhan.esg_be.global.common.enums.EsgCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "esg_score_policy")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EsgScorePolicy extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "score_rule_id")
    private Long scoreRuleId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EsgCategory category;

    @Column(name = "monthly_max_score", nullable = false)
    private Integer monthlyMaxScore;

    @Column(name = "consecutive_target_months", nullable = false)
    private Integer consecutiveTargetMonths;

    @Column(name = "consecutive_bonus_score", nullable = false)
    private Integer consecutiveBonusScore;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "base_e_score", nullable = false)
    private Integer baseEScore = 50;

    @Column(name = "base_s_score", nullable = false)
    private Integer baseSScore = 250;

    @Column(name = "base_g_activity_score", nullable = false)
    private Integer baseGActivityScore = 100;

    @Column(name = "base_g_repayment_score", nullable = false)
    private Integer baseGRepaymentScore = 100;
}
