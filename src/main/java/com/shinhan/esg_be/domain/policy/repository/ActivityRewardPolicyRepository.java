package com.shinhan.esg_be.domain.policy.repository;

import com.shinhan.esg_be.domain.policy.entity.ActivityRewardPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityRewardPolicyRepository extends JpaRepository<ActivityRewardPolicy, Long> {
}
