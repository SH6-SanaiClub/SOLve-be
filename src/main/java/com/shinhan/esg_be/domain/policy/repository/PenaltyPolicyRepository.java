package com.shinhan.esg_be.domain.policy.repository;

import com.shinhan.esg_be.domain.policy.entity.PenaltyPolicy;
import com.shinhan.esg_be.domain.policy.entity.enums.PenaltyType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PenaltyPolicyRepository extends JpaRepository<PenaltyPolicy, Long> {

    Optional<PenaltyPolicy> findByPenaltyType(PenaltyType penaltyType);
}
