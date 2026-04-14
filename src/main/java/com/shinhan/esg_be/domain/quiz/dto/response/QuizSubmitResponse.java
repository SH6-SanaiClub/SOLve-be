package com.shinhan.esg_be.domain.quiz.dto.response;

import com.shinhan.esg_be.domain.quiz.entity.QuizCategory;
import com.shinhan.esg_be.domain.quiz.entity.QuizDifficulty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class QuizSubmitResponse {

    private final String quizId;
    private final QuizCategory category;
    private final QuizDifficulty difficulty;
    private final String question;
    private final List<QuizOptionResponse> options;
    private final String selectedOptionId;
    private final String correctOptionId;
    private final Boolean correct;
    private final Integer rewardPoint;
    private final String explanation;
    private final String message;
}
