package com.shinhan.esg_be.domain.environment.repository;

import com.shinhan.esg_be.domain.environment.entity.EnvironmentActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EnvironmentActivityRepository extends JpaRepository<EnvironmentActivity, Long> {
    Optional<EnvironmentActivity> findByName(String name);
}
