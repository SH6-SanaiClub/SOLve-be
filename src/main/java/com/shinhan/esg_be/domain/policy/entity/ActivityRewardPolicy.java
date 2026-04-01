package com.shinhan.esg_be.domain.policy.entity;

import com.shinhan.esg_be.global.common.BaseTimeEntity;
import com.shinhan.esg_be.global.common.enums.ActivityType;
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

import java.math.BigDecimal;

@Entity
@Table(name = "activity_reward_policy")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActivityRewardPolicy extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reward_rule_id")
    private Long rewardRuleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 30)
    private ActivityType activityType;

    @Enumerated(EnumType.STRING)
    @Column(name = "score_category", nullable = false, length = 10)
    private EsgCategory scoreCategory;

    @Column(name = "score_value", nullable = false)
    private Integer scoreValue = 0;

    @Column(name = "point_value")
    private Integer pointValue = 0;

    @Column(name = "point_rate", precision = 5, scale = 2)
    private BigDecimal pointRate = BigDecimal.ZERO;
}
