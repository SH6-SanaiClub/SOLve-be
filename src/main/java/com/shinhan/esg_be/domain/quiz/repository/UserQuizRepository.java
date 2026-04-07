package com.shinhan.esg_be.domain.quiz.repository;

import com.shinhan.esg_be.domain.quiz.entity.UserQuiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface UserQuizRepository extends JpaRepository<UserQuiz, Long> {

    // 오늘 퀴즈 참여 여부 (하드 필터)
    @Query("""
            SELECT COUNT(uq) FROM UserQuiz uq
            WHERE uq.user.userId = :userId
              AND uq.createdAt >= :startOfDay
            """)
    long countToday(
            @Param("userId") Long userId,
            @Param("startOfDay") LocalDateTime startOfDay
    );

    // 최근 N일 퀴즈 참여 횟수 (미활동 위험, 90일 횟수)
    @Query("""
            SELECT COUNT(uq) FROM UserQuiz uq
            WHERE uq.user.userId = :userId
              AND uq.createdAt >= :since
            """)
    long countSince(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since
    );
}
