package com.shinhan.esg_be.domain.environment.repository;

import com.shinhan.esg_be.domain.environment.entity.EnvironmentActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnvironmentActivityRepository extends JpaRepository<EnvironmentActivity, Long> {

    List<EnvironmentActivity> findAllByOrderByActivityIdAsc();

    Optional<EnvironmentActivity> findByName(String name);

    boolean existsByName(String name);
}
