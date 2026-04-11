package com.shinhan.esg_be.domain.quiz.dto.response;

import com.shinhan.esg_be.domain.quiz.entity.QuizCategory;
import com.shinhan.esg_be.domain.quiz.entity.QuizDifficulty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Getter
@AllArgsConstructor
@Schema(name = "QuizSubmitResponse", description = "Quiz submit result payload")
public class QuizSubmitResponse {

    @Schema(example = "38")
    private final String quizId;
    private final QuizCategory category;
    private final QuizDifficulty difficulty;
    @Schema(example = "고정금리 대출과 변동금리 대출의 차이는 무엇인가요?")
    private final String question;
    private final List<QuizOptionResponse> options;
    @Schema(example = "1")
    private final String selectedOptionId;
    @Schema(example = "1")
    private final String correctOptionId;
    @Schema(example = "true")
    private final Boolean correct;
    @Schema(example = "10")
    private final Integer rewardPoint;
    @Schema(example = "고정금리 대출은 금리가 고정되고 변동금리는 시장 금리에 따라 변합니다.")
    private final String explanation;
    @Schema(example = "정답입니다.")
    private final String message;
}
