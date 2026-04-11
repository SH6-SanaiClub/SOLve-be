package com.shinhan.esg_be.domain.quiz.controller;

import com.shinhan.esg_be.domain.quiz.dto.request.QuizSubmitRequest;
import com.shinhan.esg_be.domain.quiz.dto.response.QuizSubmitResponse;
import com.shinhan.esg_be.domain.quiz.dto.response.QuizTodayResponse;
import com.shinhan.esg_be.domain.quiz.service.QuizService;
import com.shinhan.esg_be.global.security.AuthContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/esg/g/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;
    private final AuthContext authContext;

    @GetMapping("/today")
    @Operation(summary = "Get today's quiz", description = "Returns today's governance quiz with options array")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<QuizTodayResponse> getTodayQuiz() {
        return ResponseEntity.ok(quizService.getTodayQuiz(authContext.currentUserId()));
    }

    @PostMapping("/submit")
    @Operation(summary = "Submit today's quiz", description = "Submits selectedOptionId for today's quiz")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<QuizSubmitResponse> submitQuiz(@Valid @RequestBody QuizSubmitRequest request) {
        return ResponseEntity.ok(quizService.submitQuiz(authContext.currentUserId(), request));
    }
}
