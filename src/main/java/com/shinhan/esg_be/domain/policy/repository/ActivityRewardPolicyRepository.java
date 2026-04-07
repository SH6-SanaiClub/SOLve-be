package com.shinhan.esg_be.domain.policy.repository;

import com.shinhan.esg_be.domain.policy.entity.ActivityRewardPolicy;
import com.shinhan.esg_be.global.common.enums.ActivityType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ActivityRewardPolicyRepository extends JpaRepository<ActivityRewardPolicy, Long> {

    Optional<ActivityRewardPolicy> findByActivityType(ActivityType activityType);
}
