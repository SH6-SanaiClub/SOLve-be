package com.shinhan.esg_be.domain.policy.repository;

import com.shinhan.esg_be.domain.policy.entity.EsgScorePolicy;
import com.shinhan.esg_be.global.common.enums.ScoreCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EsgScorePolicyRepository extends JpaRepository<EsgScorePolicy, Long> {

    Optional<EsgScorePolicy> findByCategoryAndIsActiveTrue(ScoreCategory category);
}
