package com.shinhan.esg_be.domain.environment.repository;

import com.shinhan.esg_be.domain.environment.entity.EnvironmentActivity;
import com.shinhan.esg_be.domain.environment.entity.UserEnvironmentActivity;
import com.shinhan.esg_be.domain.recommendation.dto.ActivityCountProjection;
import com.shinhan.esg_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("""
            SELECT COUNT(ua) FROM UserEnvironmentActivity ua
            WHERE ua.user.userId = :userId
              AND ua.activity.activityId = :activityId
              AND ua.isApproved = true
              AND ua.createdAt >= :startOfDay
            """)
    long countTodayApproved(
            @Param("userId") Long userId,
            @Param("activityId") Long activityId,
            @Param("startOfDay") LocalDateTime startOfDay
    );

    @Query("""
            SELECT COUNT(ua) FROM UserEnvironmentActivity ua
            WHERE ua.user.userId = :userId
              AND ua.isApproved = true
              AND ua.createdAt >= :since
            """)
    long countApprovedSince(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since
    );

    @Query("""
            SELECT COUNT(ua) FROM UserEnvironmentActivity ua
            WHERE ua.user.userId = :userId
              AND ua.activity.activityId = :activityId
              AND ua.isApproved = true
              AND ua.createdAt >= :since
            """)
    long countRecentByActivity(
            @Param("userId") Long userId,
            @Param("activityId") Long activityId,
            @Param("since") LocalDateTime since
    );

    @Query("""
            SELECT ua.activity.activityId AS activityId, COUNT(ua) AS count
            FROM UserEnvironmentActivity ua
            WHERE ua.isApproved = true
              AND ua.createdAt >= :since
            GROUP BY ua.activity.activityId
            """)
    List<ActivityCountProjection> countApprovedGroupByActivitySince(
            @Param("since") LocalDateTime since
    );

    long countByActivity_ActivityId(Long activityId);
}
