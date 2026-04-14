package com.shinhan.esg_be.domain.quiz.scheduler;

import com.shinhan.esg_be.domain.quiz.service.QuizGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;

@Component
@RequiredArgsConstructor
public class QuizDailyGenerationScheduler {

    private final QuizGenerationService quizGenerationService;

    @EventListener(ApplicationReadyEvent.class)
    public void generateOnStartup() {
        quizGenerationService.ensureTodayQuizPool();
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void generateTodayQuizPool() {
        quizGenerationService.ensureTodayQuizPool();
    }
}
