package com.shinhan.esg_be.domain.policy.entity;

import com.shinhan.esg_be.domain.policy.entity.enums.PenaltyType;
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
@Table(name = "penalty_policy")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PenaltyPolicy extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "penalty_id")
    private Long penaltyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "penalty_type", nullable = false, length = 20)
    private PenaltyType penaltyType;

    @Column(name = "e_reduction", nullable = false)
    private Integer eReduction;

    @Column(name = "s_reduction", nullable = false)
    private Integer sReduction;

    @Column(name = "g_reduction", nullable = false)
    private Integer gReduction;

    @Column(name = "is_base_reduction", nullable = false)
    private Boolean isBaseReduction = false;

    @Column(name = "is_block_loan", nullable = false)
    private Boolean isBlockLoan = false;
}
