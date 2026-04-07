package com.shinhan.esg_be.domain.environment.repository;

import com.shinhan.esg_be.domain.environment.entity.EnvironmentActivity;
import com.shinhan.esg_be.domain.environment.entity.UserEnvironmentActivity;
import com.shinhan.esg_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface UserEnvironmentActivityRepository extends JpaRepository<UserEnvironmentActivity, Long> {
    boolean existsByUserAndActivityAndCreatedAtBetween(
            User user,
            EnvironmentActivity activity,
            LocalDateTime start,
            LocalDateTime end
    );

    List<UserEnvironmentActivity> findAllByUserAndCreatedAtBetween(
            User user,
            LocalDateTime start,
            LocalDateTime end
    );
}
