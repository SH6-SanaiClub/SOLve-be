package com.shinhan.esg_be.domain.recommendation.repository;

import com.shinhan.esg_be.domain.recommendation.entity.AhpWeightPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AhpWeightPolicyRepository extends JpaRepository<AhpWeightPolicy, Long> {

    List<AhpWeightPolicy> findAllByIsActiveTrue();
}