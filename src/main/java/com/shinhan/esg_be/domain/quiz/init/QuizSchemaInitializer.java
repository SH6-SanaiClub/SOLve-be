package com.shinhan.esg_be.domain.quiz.init;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuizSchemaInitializer {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void alignQuizCategoryConstraint() {
        try {
            jdbcTemplate.execute("ALTER TABLE quiz DROP CONSTRAINT IF EXISTS quiz_category_check");
            jdbcTemplate.execute("""
                    ALTER TABLE quiz
                    ADD CONSTRAINT quiz_category_check
                    CHECK (category IN (
                        'BASIC_FINANCE',
                        'SAVING',
                        'LOAN',
                        'CARD',
                        'INVESTMENT',
                        'INSURANCE'
                    ))
                    """);
            log.info("[quiz] category constraint aligned");
        } catch (Exception e) {
            log.warn("[quiz] category constraint alignment skipped", e);
        }
    }
}
