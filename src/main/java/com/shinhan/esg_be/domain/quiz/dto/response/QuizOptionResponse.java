package com.shinhan.esg_be.domain.quiz.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@AllArgsConstructor
@Schema(name = "QuizOption", description = "Quiz option item")
public class QuizOptionResponse {

    @Schema(example = "1")
    private final String id;

    @Schema(example = "이자율이 변동하는지 여부")
    private final String text;

    @Schema(example = "1")
    private final Integer order;
}
