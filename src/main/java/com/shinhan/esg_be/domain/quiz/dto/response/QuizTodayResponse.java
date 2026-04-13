package com.shinhan.esg_be.domain.quiz.dto.response;

import com.shinhan.esg_be.domain.quiz.entity.QuizCategory;
import com.shinhan.esg_be.domain.quiz.entity.QuizDifficulty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class QuizTodayResponse {

    private final String quizId;
    private final QuizCategory category;
    private final QuizDifficulty difficulty;
    private final String question;
    private final List<QuizOptionResponse> options;
    private final Boolean alreadySolved;
    private final String selectedOptionId;
    private final String correctOptionId;
    private final String explanation;
    private final Integer rewardPoint;
    private final String message;
}
