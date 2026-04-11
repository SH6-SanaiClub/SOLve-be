package com.shinhan.esg_be.domain.quiz.service;

import com.shinhan.esg_be.domain.quiz.entity.Quiz;
import com.shinhan.esg_be.domain.quiz.entity.QuizCategory;
import com.shinhan.esg_be.domain.quiz.entity.QuizDifficulty;
import com.shinhan.esg_be.domain.quiz.repository.QuizRepository;
import com.shinhan.esg_be.domain.quiz.service.client.QuizOpenAiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
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
        log.info("[quiz] ensure pool start. date={}", quizDate);
        for (QuizCategory category : QuizCategory.values()) {
            boolean missing = false;
            for (QuizDifficulty difficulty : QuizDifficulty.values()) {
                if (quizRepository.findFirstByQuizDateAndCategoryAndDifficultyAndIsActiveTrue(
                        quizDate,
                        category,
                        difficulty
                ).isEmpty()) {
                    missing = true;
                    break;
                }
            }

            if (!missing) {
                continue;
            }

            List<Quiz> generated = quizOpenAiClient.generateDailyQuizzes(quizDate, category);
            for (Quiz quiz : generated) {
                if (quizRepository.findFirstByQuizDateAndCategoryAndDifficultyAndIsActiveTrue(
                        quizDate,
                        quiz.getCategory(),
                        quiz.getDifficulty()
                ).isEmpty()) {
                    quizRepository.save(quiz);
                    log.info("[quiz] saved. date={}, category={}, difficulty={}", quizDate, quiz.getCategory(), quiz.getDifficulty());
                }
            }
        }
        log.info("[quiz] ensure pool end. date={}", quizDate);
    }

    public List<Quiz> getTodayQuizzes() {
        return quizRepository.findAllByQuizDateAndIsActiveTrueOrderByQuizIdAsc(LocalDate.now(clock));
    }
}
