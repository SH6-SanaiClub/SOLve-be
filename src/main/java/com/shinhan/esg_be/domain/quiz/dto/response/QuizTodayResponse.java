package com.shinhan.esg_be.domain.quiz.dto.response;

import com.shinhan.esg_be.domain.quiz.entity.QuizCategory;
import com.shinhan.esg_be.domain.quiz.entity.QuizDifficulty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Getter
@AllArgsConstructor
@Schema(name = "QuizTodayResponse", description = "Today's quiz payload")
public class QuizTodayResponse {

    @Schema(example = "38")
    private final String quizId;

    @Schema(example = "LOAN")
    private final QuizCategory category;

    @Schema(example = "MEDIUM")
    private final QuizDifficulty difficulty;

    @Schema(example = "고정금리 대출과 변동금리 대출의 차이는 무엇인가요?")
    private final String question;

    private final List<QuizOptionResponse> options;

    @Schema(example = "false")
    private final Boolean alreadySolved;

    private final String selectedOptionId;
    private final String correctOptionId;

    @Schema(example = "고정금리 대출은 금리가 고정되고 변동금리는 시장 금리에 따라 변합니다.")
    private final String explanation;

    @Schema(example = "10")
    private final Integer rewardPoint;

    @Schema(example = "오늘의 퀴즈를 준비했습니다.")
    private final String message;
}
