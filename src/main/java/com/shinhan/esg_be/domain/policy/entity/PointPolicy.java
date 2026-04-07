package com.shinhan.esg_be.domain.policy.entity;

import com.shinhan.esg_be.domain.point.entity.enums.PointReason;
import com.shinhan.esg_be.domain.policy.entity.enums.PointPolicyType;
import com.shinhan.esg_be.global.common.BaseTimeEntity;
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
@Table(name = "point_policy")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointPolicy extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "point_policy_id")
    private Long pointPolicyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "policy_type", nullable = false, length = 30)
    private PointPolicyType policyType;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_reason", nullable = false, length = 40)
    private PointReason targetReason;

    @Column(name = "target_count")
    private Integer targetCount;

    @Column(name = "target_days")
    private Integer targetDays;

    @Column(name = "reward_point", nullable = false)
    private Integer rewardPoint;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
