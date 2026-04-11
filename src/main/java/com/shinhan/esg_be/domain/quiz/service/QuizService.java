package com.shinhan.esg_be.domain.quiz.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shinhan.esg_be.domain.point.entity.UserPoint;
import com.shinhan.esg_be.domain.point.entity.enums.PointReason;
import com.shinhan.esg_be.domain.point.repository.UserPointRepository;
import com.shinhan.esg_be.domain.quiz.dto.request.QuizSubmitRequest;
import com.shinhan.esg_be.domain.quiz.dto.response.QuizOptionResponse;
import com.shinhan.esg_be.domain.quiz.dto.response.QuizSubmitResponse;
import com.shinhan.esg_be.domain.quiz.dto.response.QuizTodayResponse;
import com.shinhan.esg_be.domain.quiz.entity.Quiz;
import com.shinhan.esg_be.domain.quiz.entity.QuizCategory;
import com.shinhan.esg_be.domain.quiz.entity.QuizDifficulty;
import com.shinhan.esg_be.domain.quiz.entity.UserQuiz;
import com.shinhan.esg_be.domain.quiz.repository.QuizRepository;
import com.shinhan.esg_be.domain.quiz.repository.UserQuizRepository;
import com.shinhan.esg_be.domain.user.entity.User;
import com.shinhan.esg_be.domain.user.repository.UserRepository;
import com.shinhan.esg_be.global.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizService {

    private static final int QUIZ_POINT = 10;

    private final UserRepository userRepository;
    private final QuizRepository quizRepository;
    private final UserQuizRepository userQuizRepository;
    private final UserPointRepository userPointRepository;
    private final QuizGenerationService quizGenerationService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public QuizTodayResponse getTodayQuiz(Long userId) {
        User user = findUser(userId);
        LocalDate today = LocalDate.now(clock);

        quizGenerationService.ensureQuizPool(today);

        return userQuizRepository.findFirstByUserUserIdAndQuizQuizDateOrderByCreatedAtDesc(userId, today)
                .map(this::toSolvedTodayResponse)
                .orElseGet(() -> toTodayResponse(user, today));
    }

    @Transactional
    public QuizSubmitResponse submitQuiz(Long userId, QuizSubmitRequest request) {
        User user = findUser(userId);
        Long quizId = parseQuizId(request.getQuizId());
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new BadRequestException("quiz not found"));

        LocalDate today = LocalDate.now(clock);
        if (!today.equals(quiz.getQuizDate())) {
            throw new BadRequestException("only today's quiz can be submitted");
        }

        return userQuizRepository.findFirstByUserUserIdAndQuizQuizDateOrderByCreatedAtDesc(userId, today)
                .map(this::toSubmitResponse)
                .orElseGet(() -> saveQuizResult(user, quiz, request.getSelectedOptionId()));
    }

    private QuizTodayResponse toTodayResponse(User user, LocalDate today) {
        List<Quiz> quizzes = quizRepository.findAllByQuizDateAndIsActiveTrueOrderByQuizIdAsc(today);
        if (quizzes.isEmpty()) {
            throw new BadRequestException("today's quizzes are not generated yet");
        }

        Quiz selected = selectRecommendedQuiz(user.getUserId(), today, quizzes);
        List<String> choices = parseChoices(selected.getChoice());
        return new QuizTodayResponse(
                String.valueOf(selected.getQuizId()),
                selected.getCategory(),
                selected.getDifficulty(),
                selected.getQuestion(),
                toOptionResponses(choices),
                null,
                null,
                null,
                selected.getExplanation(),
                0,
                "오늘의 퀴즈를 준비했습니다."
        );
    }

    private QuizTodayResponse toSolvedTodayResponse(UserQuiz userQuiz) {
        Quiz quiz = userQuiz.getQuiz();
        boolean correct = Boolean.TRUE.equals(userQuiz.getIsCorrect());
        int point = correct ? QUIZ_POINT : 0;
        List<String> choices = parseChoices(quiz.getChoice());
        String selectedOptionId = findOptionIdByText(choices, userQuiz.getUserAnswer());
        String correctOptionId = findCorrectOptionId(choices, quiz.getAnswer());

        return new QuizTodayResponse(
                String.valueOf(quiz.getQuizId()),
                quiz.getCategory(),
                quiz.getDifficulty(),
                quiz.getQuestion(),
                toOptionResponses(choices),
                true,
                selectedOptionId,
                correctOptionId,
                quiz.getExplanation(),
                point,
                correct ? "정답입니다." : "오답입니다."
        );
    }

    private QuizSubmitResponse saveQuizResult(User user, Quiz quiz, String selectedOptionId) {
        List<String> choices = parseChoices(quiz.getChoice());
        String selectedOptionText = resolveSelectedOptionText(choices, selectedOptionId);
        boolean correct = isCorrectAnswer(quiz.getAnswer(), selectedOptionText);
        int pointDelta = correct ? QUIZ_POINT : 0;

        userQuizRepository.save(UserQuiz.create(quiz, user, correct, selectedOptionText));

        if (pointDelta > 0) {
            user.applyPoint(pointDelta);
        }
        userPointRepository.save(
                UserPoint.create(
                        user,
                        null,
                        correct ? PointReason.QUIZ_CORRECT : PointReason.QUIZ_WRONG,
                        pointDelta,
                        user.getTotalPoints()
                )
        );

        return toSubmitResponse(quiz, selectedOptionId, correct);
    }

    private QuizSubmitResponse toSubmitResponse(UserQuiz userQuiz) {
        List<String> choices = parseChoices(userQuiz.getQuiz().getChoice());
        return toSubmitResponse(
                userQuiz.getQuiz(),
                findOptionIdByText(choices, userQuiz.getUserAnswer()),
                Boolean.TRUE.equals(userQuiz.getIsCorrect())
        );
    }

    private QuizSubmitResponse toSubmitResponse(Quiz quiz, String selectedOptionId, boolean correct) {
        List<String> choices = parseChoices(quiz.getChoice());
        return new QuizSubmitResponse(
                String.valueOf(quiz.getQuizId()),
                quiz.getCategory(),
                quiz.getDifficulty(),
                quiz.getQuestion(),
                toOptionResponses(choices),
                selectedOptionId,
                findCorrectOptionId(choices, quiz.getAnswer()),
                correct,
                correct ? QUIZ_POINT : 0,
                quiz.getExplanation(),
                correct ? "정답입니다." : "오답입니다."
        );
    }

    private Quiz selectRecommendedQuiz(Long userId, LocalDate today, List<Quiz> quizzes) {
        List<UserQuiz> recentQuizzes = userQuizRepository.findTop10ByUserUserIdOrderByCreatedAtDesc(userId);
        Map<QuizCategory, CategoryStat> stats = buildStats(recentQuizzes);
        Random random = new Random(Objects.hash(userId, today));

        QuizCategory category = selectCategory(stats, recentQuizzes.size(), random);
        QuizDifficulty difficulty = selectDifficulty(stats, category, recentQuizzes.size());

        return quizzes.stream()
                .filter(quiz -> quiz.getCategory() == category && quiz.getDifficulty() == difficulty)
                .findFirst()
                .orElseGet(() -> quizzes.stream()
                        .filter(quiz -> quiz.getCategory() == category)
                        .findFirst()
                        .orElseGet(() -> quizzes.get(0)));
    }

    private QuizCategory selectCategory(Map<QuizCategory, CategoryStat> stats, int recentSize, Random random) {
        if (recentSize < 10) {
            int minExposure = stats.values().stream()
                    .mapToInt(CategoryStat::attempts)
                    .min()
                    .orElse(0);

            List<QuizCategory> candidates = stats.entrySet().stream()
                    .filter(entry -> entry.getValue().attempts() == minExposure)
                    .map(Map.Entry::getKey)
                    .toList();
            return candidates.get(random.nextInt(candidates.size()));
        }

        List<Map.Entry<QuizCategory, CategoryStat>> orderedWeakness = stats.entrySet().stream()
                .sorted(Comparator
                        .comparingDouble((Map.Entry<QuizCategory, CategoryStat> entry) -> entry.getValue().wrongRate())
                        .reversed()
                        .thenComparing((left, right) -> Integer.compare(right.getValue().attempts(), left.getValue().attempts())))
                .toList();

        List<QuizCategory> weaknessCandidates = orderedWeakness.stream()
                .limit(2)
                .map(Map.Entry::getKey)
                .toList();

        if (!weaknessCandidates.isEmpty() && random.nextDouble() < 0.7d) {
            return weaknessCandidates.get(random.nextInt(weaknessCandidates.size()));
        }

        List<QuizCategory> randomCandidates = stats.keySet().stream()
                .filter(category -> !weaknessCandidates.contains(category))
                .toList();

        if (randomCandidates.isEmpty()) {
            return weaknessCandidates.isEmpty() ? QuizCategory.BASIC_FINANCE : weaknessCandidates.get(0);
        }
        return randomCandidates.get(random.nextInt(randomCandidates.size()));
    }

    private QuizDifficulty selectDifficulty(Map<QuizCategory, CategoryStat> stats, QuizCategory category, int recentSize) {
        CategoryStat stat = stats.getOrDefault(category, CategoryStat.empty());
        if (stat.wrong() >= 2) {
            return QuizDifficulty.EASY;
        }

        if (recentSize < 10) {
            return QuizDifficulty.MEDIUM;
        }

        double accuracy = stat.totalAttempts() == 0 ? 0.0d : (double) stat.correct() / stat.totalAttempts() * 100.0d;
        if (accuracy >= 70.0d) {
            return QuizDifficulty.HARD;
        }
        if (accuracy >= 40.0d) {
            return QuizDifficulty.MEDIUM;
        }
        return QuizDifficulty.EASY;
    }

    private Map<QuizCategory, CategoryStat> buildStats(List<UserQuiz> recentQuizzes) {
        Map<QuizCategory, CategoryStat> stats = new EnumMap<>(QuizCategory.class);
        for (QuizCategory category : QuizCategory.values()) {
            stats.put(category, CategoryStat.empty());
        }

        for (UserQuiz userQuiz : recentQuizzes) {
            QuizCategory category = userQuiz.getQuiz().getCategory();
            CategoryStat current = stats.get(category);
            stats.put(category, current.add(Boolean.TRUE.equals(userQuiz.getIsCorrect())));
        }

        return stats;
    }

    private boolean isCorrectAnswer(String actual, String submitted) {
        return normalize(actual).equals(normalize(submitted));
    }

    private String resolveSelectedOptionText(List<String> choices, String selectedOptionId) {
        if (selectedOptionId == null) {
            return "";
        }

        try {
            int index = Integer.parseInt(selectedOptionId.trim()) - 1;
            if (index >= 0 && index < choices.size()) {
                return choices.get(index);
            }
        } catch (Exception ignored) {
        }

        return selectedOptionId;
    }

    private String findOptionIdByText(List<String> choices, String optionText) {
        if (optionText == null) {
            return null;
        }

        for (int i = 0; i < choices.size(); i++) {
            if (normalize(choices.get(i)).equals(normalize(optionText))) {
                return String.valueOf(i + 1);
            }
        }
        return null;
    }

    private String findCorrectOptionId(List<String> choices, String answerText) {
        return findOptionIdByText(choices, answerText);
    }

    private Long parseQuizId(String quizId) {
        try {
            return Long.parseLong(quizId);
        } catch (Exception e) {
            throw new BadRequestException("invalid quizId");
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private List<String> parseChoices(String choiceJson) {
        try {
            return objectMapper.readValue(choiceJson, new TypeReference<>() {
            });
        } catch (Exception e) {
            return List.of(choiceJson);
        }
    }

    private List<QuizOptionResponse> toOptionResponses(List<String> choices) {
        return IntStream.range(0, choices.size())
                .mapToObj(index -> new QuizOptionResponse(
                        String.valueOf(index + 1),
                        choices.get(index),
                        index + 1
                ))
                .toList();
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("user not found"));
    }

    private record CategoryStat(int attempts, int correct, int wrong) {
        static CategoryStat empty() {
            return new CategoryStat(0, 0, 0);
        }

        CategoryStat add(boolean isCorrect) {
            return new CategoryStat(
                    attempts + 1,
                    correct + (isCorrect ? 1 : 0),
                    wrong + (isCorrect ? 0 : 1)
            );
        }

        double wrongRate() {
            return attempts == 0 ? 0.0d : (double) wrong / attempts;
        }

        int totalAttempts() {
            return attempts;
        }
    }
}
