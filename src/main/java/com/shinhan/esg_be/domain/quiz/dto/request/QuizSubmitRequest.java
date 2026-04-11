package com.shinhan.esg_be.domain.quiz.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class QuizSubmitRequest {

    @NotBlank(message = "quizId is required.")
    private String quizId;

    @NotBlank(message = "selectedOptionId is required.")
    private String selectedOptionId;
}
