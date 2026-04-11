package com.shinhan.esg_be.domain.quiz.repository;

import com.shinhan.esg_be.domain.quiz.entity.Quiz;
import com.shinhan.esg_be.domain.quiz.entity.QuizCategory;
import com.shinhan.esg_be.domain.quiz.entity.QuizDifficulty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    List<Quiz> findAllByQuizDateAndIsActiveTrueOrderByQuizIdAsc(LocalDate quizDate);

    Optional<Quiz> findFirstByQuizDateAndCategoryAndDifficultyAndIsActiveTrue(
            LocalDate quizDate,
            QuizCategory category,
            QuizDifficulty difficulty
    );

    boolean existsByQuizDateAndIsActiveTrue(LocalDate quizDate);

    @Query("""
            SELECT q
            FROM Quiz q
            WHERE q.isActive = true
              AND q.createdAt >= :startOfDay
              AND q.createdAt < :nextDay
            ORDER BY q.createdAt DESC
            """)
    List<Quiz> findTodayActiveQuizList(
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("nextDay") LocalDateTime nextDay
    );

    default Optional<Quiz> findTodayActiveQuiz(LocalDateTime now) {
        LocalDate today = now.toLocalDate();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime nextDay = today.plusDays(1).atStartOfDay();
        return findTodayActiveQuizList(startOfDay, nextDay).stream().findFirst();
    }
}
