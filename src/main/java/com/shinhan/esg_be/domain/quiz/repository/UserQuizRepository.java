package com.shinhan.esg_be.domain.quiz.repository;

import com.shinhan.esg_be.domain.quiz.entity.UserQuiz;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserQuizRepository extends JpaRepository<UserQuiz, Long> {

    @Query("""
            SELECT COUNT(uq) FROM UserQuiz uq
            WHERE uq.user.userId = :userId
              AND uq.createdAt >= :startOfDay
            """)
    long countToday(
            @Param("userId") Long userId,
            @Param("startOfDay") LocalDateTime startOfDay
    );

    @Query("""
            SELECT COUNT(uq) FROM UserQuiz uq
            WHERE uq.user.userId = :userId
              AND uq.createdAt >= :since
            """)
    long countSince(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since
    );

    long countByCreatedAtAfter(LocalDateTime since);

    long countByUserUserId(Long userId);

    @EntityGraph(attributePaths = "quiz")
    List<UserQuiz> findByUserUserIdOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = "quiz")
    List<UserQuiz> findTop10ByUserUserIdOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = "quiz")
    Optional<UserQuiz> findFirstByUserUserIdAndCreatedAtAfterOrderByCreatedAtDesc(Long userId, LocalDateTime startOfDay);

    boolean existsByUserUserIdAndCreatedAtAfter(Long userId, LocalDateTime startOfDay);
}
