package com.shinhan.esg_be.domain.quiz.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.shinhan.esg_be.domain.reward.service.RewardService;
import com.shinhan.esg_be.domain.reward.service.command.ApplyActivityRewardCommand;
import com.shinhan.esg_be.domain.reward.service.result.ApplyActivityRewardResult;
import com.shinhan.esg_be.domain.point.entity.UserPoint;
import com.shinhan.esg_be.domain.point.entity.enums.PointReason;
import com.shinhan.esg_be.domain.point.repository.UserPointRepository;
import com.shinhan.esg_be.domain.score.entity.ValidScoreHistory;
import com.shinhan.esg_be.domain.score.repository.ValidScoreHistoryRepository;
import com.shinhan.esg_be.domain.stat.entity.UserMonthlyStat;
import com.shinhan.esg_be.domain.stat.repository.UserMonthlyStatRepository;
import com.shinhan.esg_be.domain.user.entity.User;
import com.shinhan.esg_be.domain.user.repository.UserRepository;
import com.shinhan.esg_be.global.common.enums.ActivityType;
import com.shinhan.esg_be.global.common.enums.ScoreCategory;
import com.shinhan.esg_be.global.common.enums.ScoreReason;
import com.shinhan.esg_be.global.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;

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

    private final UserRepository userRepository;
    private final QuizRepository quizRepository;
    private final UserQuizRepository userQuizRepository;
    private final QuizGenerationService quizGenerationService;
    private final RewardService rewardService;
    private final UserMonthlyStatRepository userMonthlyStatRepository;
    private final ValidScoreHistoryRepository validScoreHistoryRepository;
    private final UserPointRepository userPointRepository;
    private final PlatformTransactionManager transactionManager;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public QuizTodayResponse getTodayQuiz(Long userId) {
        User user = findUser(userId);
        LocalDate today = LocalDate.now(clock);
        int totalAttempts = Math.toIntExact(userQuizRepository.countByUserUserId(userId));

        List<Quiz> quizzes = quizRepository.findAllByQuizDateAndIsActiveTrueOrderByQuizIdAsc(today);
        if (quizzes.isEmpty()) {
            quizGenerationService.ensureQuizPool(today);
            quizzes = quizRepository.findAllByQuizDateAndIsActiveTrueOrderByQuizIdAsc(today);
        }
        List<Quiz> todayQuizzes = quizzes;
        List<UserQuiz> history = totalAttempts < 12
                ? userQuizRepository.findByUserUserIdOrderByCreatedAtDesc(userId)
                : userQuizRepository.findTop10ByUserUserIdOrderByCreatedAtDesc(userId);

        return userQuizRepository.findFirstByUserUserIdAndCreatedAtAfterOrderByCreatedAtDesc(userId, today.atStartOfDay())
                .map(this::toSolvedTodayResponse)
                .orElseGet(() -> toTodayResponse(user, today, todayQuizzes, history, totalAttempts));
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

        return userQuizRepository.findFirstByUserUserIdAndCreatedAtAfterOrderByCreatedAtDesc(userId, today.atStartOfDay())
                .map(this::toSubmitResponse)
                .orElseGet(() -> saveQuizResult(user, quiz, request.getSelectedOptionId()));
    }

    private QuizTodayResponse toTodayResponse(User user, LocalDate today, List<Quiz> quizzes, List<UserQuiz> history, int totalAttempts) {
        if (quizzes.isEmpty()) {
            throw new BadRequestException("today's quizzes are not generated yet");
        }

        Quiz selected = selectRecommendedQuiz(user.getUserId(), today, quizzes, history, totalAttempts);
        List<String> choices = parseChoices(selected.getChoice());
        return new QuizTodayResponse(
                String.valueOf(selected.getQuizId()),
                selected.getCategory(),
                selected.getDifficulty(),
                selected.getQuestion(),
                toOptionResponses(choices),
                false,
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
        int point = correct ? 20 : 10;
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
                "이미 오늘의 퀴즈를 완료했습니다."
        );
    }

    private QuizSubmitResponse saveQuizResult(User user, Quiz quiz, String selectedOptionId) {
        List<String> choices = parseChoices(quiz.getChoice());
        String selectedOptionText = resolveSelectedOptionText(choices, selectedOptionId);
        boolean correct = isCorrectAnswer(quiz.getAnswer(), selectedOptionText);

        userQuizRepository.save(UserQuiz.create(quiz, user, correct, selectedOptionText));

        int rewardPoint = applyQuizRewardWithFallback(user, correct);

        return toSubmitResponse(quiz, selectedOptionId, correct, rewardPoint);
    }

    private int applyQuizRewardWithFallback(User user, boolean correct) {
        try {
            TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
            transactionTemplate.setPropagationBehaviorName("PROPAGATION_REQUIRES_NEW");
            ApplyActivityRewardResult rewardResult = transactionTemplate.execute(status ->
                    rewardService.applyActivityReward(
                            new ApplyActivityRewardCommand(
                                    user.getUserId(),
                                    correct ? ActivityType.QUIZ_CORRECT : ActivityType.QUIZ_WRONG,
                                    null,
                                    LocalDate.now(clock).atStartOfDay()
                            )
                    )
            );

            if (rewardResult != null) {
                return rewardResult.pointResult().activityPoint() + rewardResult.pointResult().bonusPoint();
            }
        } catch (RuntimeException ignored) {
            // fall back to local handling below
        }

        return applyQuizFallback(user, correct);
    }

    private int applyQuizFallback(User user, boolean correct) {
        LocalDate today = LocalDate.now(clock);
        int rewardPoint = correct ? 20 : 10;

        UserMonthlyStat monthlyStat = userMonthlyStatRepository.findByUser(user)
                .orElseGet(() -> userMonthlyStatRepository.save(UserMonthlyStat.create(user)));

        if (monthlyStat.getMonthlyScore(ScoreCategory.G_ACTIVITY) < 10) {
            user.applyScore(ScoreCategory.G_ACTIVITY, 1);
            user.updateLastActivityDate(today.atStartOfDay());
            monthlyStat.addScore(ScoreCategory.G_ACTIVITY, 1);
            validScoreHistoryRepository.save(
                    ValidScoreHistory.create(
                            user,
                            ScoreCategory.G_ACTIVITY,
                            1,
                            ScoreReason.QUIZ,
                            today.plusYears(1).atStartOfDay(),
                            user.getScore(ScoreCategory.G_ACTIVITY)
                    )
            );
        }

        user.applyPoint(rewardPoint);
        userPointRepository.save(
                UserPoint.create(
                        user,
                        null,
                        correct ? PointReason.QUIZ_CORRECT : PointReason.QUIZ_WRONG,
                        rewardPoint,
                        user.getTotalPoints()
                )
        );
        return rewardPoint;
    }

    private QuizSubmitResponse toSubmitResponse(UserQuiz userQuiz) {
        List<String> choices = parseChoices(userQuiz.getQuiz().getChoice());
        boolean correct = Boolean.TRUE.equals(userQuiz.getIsCorrect());
        return toSubmitResponse(
                userQuiz.getQuiz(),
                findOptionIdByText(choices, userQuiz.getUserAnswer()),
                correct,
                correct ? 20 : 10
        );
    }

    private QuizSubmitResponse toSubmitResponse(Quiz quiz, String selectedOptionId, boolean correct, int rewardPoint) {
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
                rewardPoint,
                quiz.getExplanation(),
                correct ? "정답입니다." : "오답입니다."
        );
    }

    private Quiz selectRecommendedQuiz(Long userId, LocalDate today, List<Quiz> quizzes, List<UserQuiz> history, int totalAttempts) {
        Map<QuizCategory, CategoryStat> stats = buildStats(history);
        Random random = new Random(Objects.hash(userId, today));

        QuizCategory category = totalAttempts < 12
                ? selectExplorationCategory(stats, random)
                : selectCategory(stats, history.size(), random);
        QuizDifficulty difficulty = totalAttempts < 12
                ? QuizDifficulty.MEDIUM
                : selectDifficulty(stats, category, history.size());

        return quizzes.stream()
                .filter(quiz -> quiz.getCategory() == category && quiz.getDifficulty() == difficulty)
                .findFirst()
                .orElseGet(() -> quizzes.stream()
                        .filter(quiz -> quiz.getCategory() == category)
                        .findFirst()
                        .orElseGet(() -> quizzes.get(0)));
    }

    private QuizCategory selectExplorationCategory(Map<QuizCategory, CategoryStat> stats, Random random) {
        List<QuizCategory> candidates = stats.entrySet().stream()
                .filter(entry -> entry.getValue().attempts() < 3)
                .map(Map.Entry::getKey)
                .toList();
        return candidates.get(random.nextInt(candidates.size()));
    }

    private QuizCategory selectCategory(Map<QuizCategory, CategoryStat> stats, int recentSize, Random random) {
        if (recentSize < 12) {
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

        if (recentSize < 12) {
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

    private Long parseQuizId(String quizId) {
        try {
            return Long.parseLong(quizId);
        } catch (Exception e) {
            throw new BadRequestException("invalid quizId");
        }
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
