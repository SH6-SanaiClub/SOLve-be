package com.shinhan.esg_be.domain.policy.repository;

import com.shinhan.esg_be.domain.policy.entity.PointPolicy;
import com.shinhan.esg_be.domain.policy.entity.enums.PointPolicyType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PointPolicyRepository extends JpaRepository<PointPolicy, Long> {

    Optional<PointPolicy> findByPolicyTypeAndIsActiveTrue(PointPolicyType policyType);
}
