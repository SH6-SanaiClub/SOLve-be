package com.shinhan.esg_be.domain.quiz.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class QuizOptionResponse {

    private final String id;
    private final String text;
    private final Integer order;
}
