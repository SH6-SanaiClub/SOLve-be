package com.shinhan.esg_be.domain.activity.repository;

import com.shinhan.esg_be.domain.activity.entity.UserActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface UserActivityRepository extends JpaRepository<UserActivity, Long> {

    // 오늘 특정 activity 승인 여부 (하드 필터 - 일일 한도)
    @Query("""
            SELECT COUNT(ua) FROM UserActivity ua
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

    // 최근 N일 승인된 E활동 횟수 (미활동 위험, 90일 횟수)
    @Query("""
            SELECT COUNT(ua) FROM UserActivity ua
            WHERE ua.user.userId = :userId
              AND ua.isApproved = true
              AND ua.createdAt >= :since
            """)
    long countApprovedSince(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since
    );

    // 최근 14일 특정 activity 수행 횟수 (피로도 감점)
    @Query("""
            SELECT COUNT(ua) FROM UserActivity ua
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
}
