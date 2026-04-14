package com.shinhan.esg_be.domain.quiz.entity;

import com.shinhan.esg_be.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

@Entity
@Table(name = "quiz")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Quiz extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quiz_id")
    private Long quizId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String choice;

    @Column(nullable = false)
    private String answer;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String explanation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private QuizCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuizDifficulty difficulty;

    @Column(name = "quiz_date", nullable = false)
    private LocalDate quizDate;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    public static Quiz create(
            String question,
            String choice,
            String answer,
            String explanation,
            QuizCategory category,
            QuizDifficulty difficulty,
            LocalDate quizDate
    ) {
        Quiz quiz = new Quiz();
        quiz.update(question, choice, answer, explanation, category, difficulty, quizDate);
        return quiz;
    }

    public void update(
            String question,
            String choice,
            String answer,
            String explanation,
            QuizCategory category,
            QuizDifficulty difficulty,
            LocalDate quizDate
    ) {
        this.question = question;
        this.choice = choice;
        this.answer = answer;
        this.explanation = explanation;
        this.category = category;
        this.difficulty = difficulty;
        this.quizDate = quizDate;
        this.isActive = true;
    }
}
