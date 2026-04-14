package com.shinhan.esg_be.domain.quiz.controller;

import com.shinhan.esg_be.domain.quiz.dto.request.QuizSubmitRequest;
import com.shinhan.esg_be.domain.quiz.dto.response.QuizSubmitResponse;
import com.shinhan.esg_be.domain.quiz.dto.response.QuizTodayResponse;
import com.shinhan.esg_be.domain.quiz.service.QuizService;
import com.shinhan.esg_be.global.security.AuthContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
    public ResponseEntity<QuizTodayResponse> getTodayQuiz() {
        return ResponseEntity.ok(quizService.getTodayQuiz(authContext.currentUserId()));
    }

    @PostMapping("/submit")
    public ResponseEntity<QuizSubmitResponse> submitQuiz(@Valid @RequestBody QuizSubmitRequest request) {
        return ResponseEntity.ok(quizService.submitQuiz(authContext.currentUserId(), request));
    }
}
