package com.shinhan.esg_be.domain.quiz.service;

import com.shinhan.esg_be.domain.quiz.entity.Quiz;
import com.shinhan.esg_be.domain.quiz.entity.QuizCategory;
import com.shinhan.esg_be.domain.quiz.entity.QuizDifficulty;
import com.shinhan.esg_be.domain.quiz.repository.QuizRepository;
import com.shinhan.esg_be.domain.quiz.service.client.QuizOpenAiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuizGenerationService {

    private final QuizRepository quizRepository;
    private final QuizOpenAiClient quizOpenAiClient;
    private final Clock clock;

    @Transactional
    public void ensureTodayQuizPool() {
        ensureQuizPool(LocalDate.now(clock));
    }

    @Transactional
    public void ensureQuizPool(LocalDate quizDate) {
        for (QuizCategory category : QuizCategory.values()) {
            List<Quiz> generated = quizOpenAiClient.generateDailyQuizzes(quizDate, category);
            for (Quiz quiz : generated) {
                upsertQuiz(quiz);
            }
        }
    }

    private void upsertQuiz(Quiz generated) {
        quizRepository.findFirstByCategoryAndDifficultyAndIsActiveTrueOrderByQuizIdAsc(
                        generated.getCategory(),
                        generated.getDifficulty()
                )
                .ifPresentOrElse(
                        existing -> existing.update(
                                generated.getQuestion(),
                                generated.getChoice(),
                                generated.getAnswer(),
                                generated.getExplanation(),
                                generated.getCategory(),
                                generated.getDifficulty(),
                                generated.getQuizDate()
                        ),
                        () -> quizRepository.save(generated)
                );
    }

}
