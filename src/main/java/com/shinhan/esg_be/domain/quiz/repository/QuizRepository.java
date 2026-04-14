package com.shinhan.esg_be.domain.quiz.repository;

import com.shinhan.esg_be.domain.quiz.entity.Quiz;
import com.shinhan.esg_be.domain.quiz.entity.QuizCategory;
import com.shinhan.esg_be.domain.quiz.entity.QuizDifficulty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    List<Quiz> findAllByQuizDateAndIsActiveTrueOrderByQuizIdAsc(LocalDate quizDate);

    Optional<Quiz> findFirstByCategoryAndDifficultyAndIsActiveTrueOrderByQuizIdAsc(
            QuizCategory category,
            QuizDifficulty difficulty
    );

    Optional<Quiz> findFirstByQuizDateAndIsActiveTrueOrderByQuizIdAsc(LocalDate quizDate);
}
