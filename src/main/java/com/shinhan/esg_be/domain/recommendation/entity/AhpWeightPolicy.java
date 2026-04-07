package com.shinhan.esg_be.domain.recommendation.entity;

import com.shinhan.esg_be.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ahp_weight_policy")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AhpWeightPolicy extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "weight_id")
    private Long weightId;

    @Column(name = "criterion_code", nullable = false, length = 30)
    private String criterionCode;

    @Column(name = "criterion_name", nullable = false, length = 50)
    private String criterionName;

    @Column(nullable = false)
    private Double weight;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}