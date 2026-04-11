package com.shinhan.esg_be.domain.quiz.service.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shinhan.esg_be.domain.quiz.entity.Quiz;
import com.shinhan.esg_be.domain.quiz.entity.QuizCategory;
import com.shinhan.esg_be.domain.quiz.entity.QuizDifficulty;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Slf4j
public class QuizOpenAiClient {

    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    @Value("${openai.model}")
    private String model;

    public QuizOpenAiClient(
            ObjectMapper objectMapper,
            @Qualifier("openAiRestClient") RestClient restClient
    ) {
        this.objectMapper = objectMapper;
        this.restClient = restClient;
    }

    public List<Quiz> generateDailyQuizzes(LocalDate quizDate, QuizCategory category) {
        String raw = callOpenAi(buildSystemPrompt(), buildUserPrompt(quizDate, category));
        List<Quiz> parsed = parseResponse(raw, quizDate, category);
        if (parsed.isEmpty()) {
            log.warn("[quiz-ai] fallback used. date={}, category={}", quizDate, category);
            return buildFallbackQuizzes(quizDate, category);
        }

        log.info("[quiz-ai] openai generated quizzes. date={}, category={}, count={}", quizDate, category, parsed.size());
        return parsed;
    }

    private String callOpenAi(String systemPrompt, String userPrompt) {
        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)
                    ),
                    "temperature", 0.4,
                    "max_tokens", 1200,
                    "response_format", Map.of("type", "json_object")
            );

            Map<?, ?> response = restClient.post()
                    .uri("/v1/chat/completions")
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                return null;
            }

            List<?> choices = (List<?>) response.get("choices");
            if (choices == null || choices.isEmpty()) {
                return null;
            }

            Map<?, ?> choice = (Map<?, ?>) choices.get(0);
            Map<?, ?> message = (Map<?, ?>) choice.get("message");
            return message == null ? null : (String) message.get("content");
        } catch (Exception e) {
            log.warn("[quiz-ai] openai call failed. date prompt request will fallback", e);
            return null;
        }
    }

    private List<Quiz> parseResponse(String raw, LocalDate quizDate, QuizCategory category) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }

        try {
            JsonNode root = objectMapper.readTree(stripCodeFence(raw));
            JsonNode quizzesNode = root.path("quizzes");
            if (!quizzesNode.isArray()) {
                return List.of();
            }

            List<Quiz> quizzes = new ArrayList<>();
            for (JsonNode node : quizzesNode) {
                QuizDifficulty difficulty = parseDifficulty(node.path("difficulty").asText(null));
                String question = node.path("question").asText(null);
                String answer = node.path("answer").asText(null);
                String explanation = node.path("explanation").asText(null);
                JsonNode choicesNode = node.path("choices");

                if (difficulty == null || question == null || answer == null || explanation == null || !choicesNode.isArray()) {
                    continue;
                }

                List<String> choices = new ArrayList<>();
                for (JsonNode choiceNode : choicesNode) {
                    choices.add(choiceNode.asText());
                }

                if (choices.size() < 4 || choices.stream().noneMatch(choice -> choice.equals(answer))) {
                    continue;
                }

                quizzes.add(Quiz.create(
                        question,
                        toJsonArray(choices),
                        answer,
                        explanation,
                        category,
                        difficulty,
                        quizDate
                ));
            }
            return quizzes;
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<Quiz> buildFallbackQuizzes(LocalDate quizDate, QuizCategory category) {
        return List.of(
                buildFallbackQuiz(quizDate, category, QuizDifficulty.EASY),
                buildFallbackQuiz(quizDate, category, QuizDifficulty.MEDIUM),
                buildFallbackQuiz(quizDate, category, QuizDifficulty.HARD)
        );
    }

    private Quiz buildFallbackQuiz(LocalDate quizDate, QuizCategory category, QuizDifficulty difficulty) {
        return switch (category) {
            case BASIC_FINANCE -> fallback(
                    quizDate,
                    category,
                    difficulty,
                    "What is the main difference between a deposit and an installment savings account?",
                    List.of(
                            "A deposit is usually made at once, while installment savings are made over time",
                            "A deposit is made monthly, while installment savings are made once",
                            "They are exactly the same",
                            "Installment savings do not earn interest"
                    ),
                    "A deposit is usually made at once, while installment savings are made over time",
                    "A deposit is typically a lump-sum savings product, while installment savings are paid in regular installments."
            );
            case SAVING -> fallback(
                    quizDate,
                    category,
                    difficulty,
                    "Which habit best helps build a saving routine?",
                    List.of(
                            "Save a fixed amount first every month",
                            "Only save what is left after spending",
                            "Do not make any saving plan",
                            "Spend the card bill first"
                    ),
                    "Save a fixed amount first every month",
                    "Automating savings before discretionary spending helps build a stable habit."
            );
            case LOAN -> fallback(
                    quizDate,
                    category,
                    difficulty,
                    "What should you check first when comparing loan products?",
                    List.of(
                            "Interest rate and repayment method",
                            "The advertisement copy",
                            "The product name",
                            "The branch location"
                    ),
                    "Interest rate and repayment method",
                    "Interest rate, repayment period, and repayment method directly affect the loan burden."
            );
            case CARD -> fallback(
                    quizDate,
                    category,
                    difficulty,
                    "Which statement best describes a debit card?",
                    List.of(
                            "It is charged immediately from the linked account",
                            "It is paid from the linked account later",
                            "It can only be used for foreign purchases",
                            "It only supports installment payments"
                    ),
                    "It is charged immediately from the linked account",
                    "A debit card deducts money from the linked account at the time of payment."
            );
            case INVESTMENT -> fallback(
                    quizDate,
                    category,
                    difficulty,
                    "What is the purpose of diversification?",
                    List.of(
                            "To put all risk into one asset",
                            "To make investment returns always zero",
                            "To spread risk across multiple assets",
                            "To avoid investing altogether"
                    ),
                    "To spread risk across multiple assets",
                    "Diversification is a common way to reduce the impact of volatility in one asset."
            );
            case INSURANCE -> fallback(
                    quizDate,
                    category,
                    difficulty,
                    "What is the basic role of insurance?",
                    List.of(
                            "To prepare for unexpected risks",
                            "To guarantee profit",
                            "To eliminate all spending",
                            "To remove taxes"
                    ),
                    "To prepare for unexpected risks",
                    "Insurance works as a safety net when unexpected losses occur."
            );
        };
    }

    private Quiz fallback(
            LocalDate quizDate,
            QuizCategory category,
            QuizDifficulty difficulty,
            String question,
            List<String> choices,
            String answer,
            String explanation
    ) {
        return Quiz.create(question, toJsonArray(choices), answer, explanation, category, difficulty, quizDate);
    }

    private String buildSystemPrompt() {
        return """
                You are a finance quiz generator.
                Return exactly one JSON object.
                The schema must be:
                {
                  "quizzes": [
                    {
                      "category": "BASIC_FINANCE",
                      "difficulty": "EASY",
                      "question": "question text",
                      "choices": ["choice1", "choice2", "choice3", "choice4"],
                      "answer": "exactly one of the choices",
                      "explanation": "short explanation"
                    }
                  ]
                }
                Rules:
                - each quiz must have at least 4 choices
                - answer must match one choice exactly
                - category must match the requested category
                - difficulty must be one of EASY, MEDIUM, HARD
                - write natural Korean quiz content
                """;
    }

    private String buildUserPrompt(LocalDate quizDate, QuizCategory category) {
        return String.format("""
                Generate 3 quizzes for %s on %s.
                Create exactly one EASY, one MEDIUM, and one HARD quiz.
                Make the topics different from each other.
                Keep each explanation within 2 sentences.
                """, category, quizDate);
    }

    private QuizDifficulty parseDifficulty(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return QuizDifficulty.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return null;
        }
    }

    private String stripCodeFence(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            int firstLineBreak = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstLineBreak >= 0 && lastFence > firstLineBreak) {
                return trimmed.substring(firstLineBreak + 1, lastFence).trim();
            }
        }
        return trimmed;
    }

    private String toJsonArray(List<String> choices) {
        try {
            return objectMapper.writeValueAsString(choices);
        } catch (Exception e) {
            return choices.toString();
        }
    }
}
