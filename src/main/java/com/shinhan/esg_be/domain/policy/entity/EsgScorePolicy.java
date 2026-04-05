package com.shinhan.esg_be.domain.policy.entity;

import com.shinhan.esg_be.global.common.BaseTimeEntity;
import com.shinhan.esg_be.global.common.enums.ScoreCategory;
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
    @Column(nullable = false, length = 20)
    private ScoreCategory category;

    @Column(name = "monthly_max_score")
    private Integer monthlyMaxScore;

    @Column(name = "max_score", nullable = false)
    private Integer maxScore;

    @Column(name = "consecutive_target_months")
    private Integer consecutiveTargetMonths;

    @Column(name = "consecutive_bonus_score")
    private Integer consecutiveBonusScore;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "base_score", nullable = false)
    private Integer baseScore;
}
