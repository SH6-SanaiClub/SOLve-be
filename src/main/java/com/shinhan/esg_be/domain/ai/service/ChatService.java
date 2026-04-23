package com.shinhan.esg_be.domain.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shinhan.esg_be.domain.ai.dto.ChatAction;
import com.shinhan.esg_be.domain.ai.dto.ChatUserContext;
import com.shinhan.esg_be.domain.bank.dto.response.SavingsRecommendResponse;
import com.shinhan.esg_be.domain.bank.service.FinanceRecommendService;
import com.shinhan.esg_be.domain.environment.entity.UserEnvironmentActivity;
import com.shinhan.esg_be.domain.environment.repository.UserEnvironmentActivityRepository;
import com.shinhan.esg_be.domain.recommendation.dto.ActivityRecommendResponse;
import com.shinhan.esg_be.domain.recommendation.dto.ActivityRecommendResponse.RecommendedActivity;
import com.shinhan.esg_be.domain.recommendation.dto.UserFeatureDto;
import com.shinhan.esg_be.domain.recommendation.service.FeatureExtractor;
import com.shinhan.esg_be.domain.recommendation.service.RecommendCacheService;
import com.shinhan.esg_be.domain.recommendation.service.RecommendationService;
import com.shinhan.esg_be.domain.quiz.repository.UserQuizRepository;
import com.shinhan.esg_be.domain.social.repository.UserDonationRepository;
import com.shinhan.esg_be.domain.social.repository.UserEcoProductRepository;
import com.shinhan.esg_be.domain.user.entity.User;
import com.shinhan.esg_be.domain.user.repository.UserRepository;
import com.shinhan.esg_be.domain.volunteer.repository.UserVolunteerRepository;
import com.shinhan.esg_be.global.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final String ACTIONS_START_MARKER = "[ACTIONS]";
    private static final String ACTIONS_END_MARKER = "[/ACTIONS]";
    private static final String INTENT_FINANCE = "finance";
    private static final String INTENT_ACTIVITY = "activity";
    private static final String INTENT_POPULAR = "popular";
    private static final String INTENT_STATUS = "status";
    private static final String INTENT_GRADE = "grade";
    private static final String INTENT_DEFAULT = "default";
    private static final int MAX_ACTIONS = 2;
    private static final int MAX_SUGGESTIONS = 4;
    private static final int STREAM_MIN_FLUSH_CHARS = 24;
    private static final int STREAM_FORCE_FLUSH_CHARS = 72;
    private static final long SMALL_TALK_CHUNK_DELAY_MS = 120L;
    private static final int LOAN_MIN_ESG_SCORE = 700;
    private static final List<String> ENVIRONMENT_ACTIVITY_NAMES = List.of(
            "텀블러 인증",
            "공유자전거 인증",
            "전기차 인증"
    );
    private static final Set<String> ALLOWED_ACTION_PATHS = Set.of(
            "/finance",
            "/finance/green-step-up-savings",
            "/finance/earth-guardian-savings",
            "/finance/warm-companion-savings",
            "/finance/smart-finance-savings",
            "/finance/esg-master-savings",
            "/finance/esg-micro-loan",
            "/activities/environment",
            "/esg/social",
            "/esg/social/donation",
            "/esg/social/store",
            "/esg/social/volunteer",
            "/esg/quiz",
            "/activities/governance",
            "/my",
            "/my/grade"
    );
    private static final Set<String> ACTIVITY_ACTION_PATHS = Set.of(
            "/activities/environment",
            "/esg/social",
            "/esg/social/donation",
            "/esg/social/store",
            "/esg/social/volunteer",
            "/esg/quiz",
            "/activities/governance"
    );
    private static final Set<String> STATUS_ACTION_PATHS = Set.of(
            "/my",
            "/my/grade",
            "/activities/environment",
            "/esg/social/donation",
            "/esg/social/store",
            "/esg/social/volunteer",
            "/esg/quiz"
    );

    private final LLMClient llmClient;
    private final ChatContextService contextService;
    private final ChatHistoryService historyService;
    private final RecommendationService recommendationService;
    private final RecommendCacheService recommendCacheService;
    private final FinanceRecommendService financeRecommendService;
    private final FeatureExtractor featureExtractor;
    private final UserEnvironmentActivityRepository userEnvironmentActivityRepository;
    private final UserDonationRepository userDonationRepository;
    private final UserVolunteerRepository userVolunteerRepository;
    private final UserEcoProductRepository userEcoProductRepository;
    private final UserQuizRepository userQuizRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    @Qualifier("chatExecutor")
    private final ThreadPoolTaskExecutor chatExecutor;

    private String cachedStaticPolicy = null;

    public SseEmitter streamChat(Long userId, String userMessage) {
        SseEmitter emitter = new SseEmitter(60_000L);

        chatExecutor.execute(() -> {
            try {
                String normalizedUserMessage = normalizeKeywordSource(userMessage);
                String intent = detectIntent(normalizedUserMessage, normalizedUserMessage);
                long historyVersion = historyService.getHistoryVersion(userId);
                if (handleSmallTalkFastPath(emitter, userId, userMessage, normalizedUserMessage, intent, historyVersion)) {
                    return;
                }
                String staticPolicy = getStaticPolicy();
                ChatUserContext context = contextService.getOrLoad(userId);
                GroundedTurn groundedTurn = buildGroundedTurn(userId, userMessage, normalizedUserMessage, intent, context);
                String latestAssistantMeta = historyService.getLatestAssistantMetaPrompt(userId);

                String systemPrompt = buildSystemPrompt(staticPolicy, context, groundedTurn, latestAssistantMeta);
                List<Map<String, String>> history = historyService.getPromptHistory(userId);
                List<Map<String, String>> messages = buildMessages(systemPrompt, history, userMessage);

                StringBuilder fullResponse = new StringBuilder();
                StringBuilder visibleBuffer = new StringBuilder();
                boolean[] actionBlockStarted = {false};

                llmClient.stream(
                        messages,
                        chunk -> {
                            fullResponse.append(chunk);

                            if (actionBlockStarted[0]) {
                                return;
                            }

                            visibleBuffer.append(chunk);
                            int actionStartIndex = visibleBuffer.indexOf(ACTIONS_START_MARKER);
                            if (actionStartIndex >= 0) {
                                flushVisibleText(
                                        emitter,
                                        visibleBuffer.substring(0, actionStartIndex)
                                );
                                visibleBuffer.setLength(0);
                                actionBlockStarted[0] = true;
                                return;
                            }

                            flushVisibleBuffer(emitter, visibleBuffer);
                        },
                        () -> {
                            String response = fullResponse.toString();
                            String cleanResponse = normalizeAssistantText(removeActionsBlock(response));

                            if (!actionBlockStarted[0]) {
                                flushAllVisibleBuffer(emitter, visibleBuffer);
                            }

                            sendEvent(emitter, "replace", cleanResponse);

                            List<ChatAction> actions = resolveActions(
                                    intent,
                                    groundedTurn.preferredActions(),
                                    parseActions(response),
                                    userMessage,
                                    response
                            );
                            if (!actions.isEmpty()) {
                                sendEvent(emitter, "actions", serialize(actions));
                            }

                            List<String> suggestions = resolveSuggestions(
                                    groundedTurn.suggestions(),
                                    intent,
                                    cleanResponse,
                                    actions
                            );
                            if (!suggestions.isEmpty()) {
                                sendEvent(emitter, "suggestions", serialize(suggestions));
                            }

                            historyService.appendAndSave(
                                    userId,
                                    userMessage,
                                    cleanResponse,
                                    actions,
                                    suggestions,
                                    historyVersion
                            );

                            sendEvent(emitter, "done", "");
                            emitter.complete();
                        }
                );
            } catch (Exception e) {
                log.error("챗봇 스트리밍 실패 userId={}", userId, e);
                sendEvent(emitter, "error", "일시적인 오류가 발생했어요. 다시 시도해주세요.");
                emitter.complete();
            }
        });

        return emitter;
    }

    private boolean handleSmallTalkFastPath(
            SseEmitter emitter,
            Long userId,
            String userMessage,
            String normalizedUserMessage,
            String intent,
            long historyVersion
    ) {
        if (!INTENT_DEFAULT.equals(intent)) {
            return false;
        }

        String response = buildSmallTalkResponse(normalizedUserMessage);
        if (response == null) {
            return false;
        }

        List<String> suggestions = List.of(
                "내 점수/등급 알려줘",
                "오늘 추천 활동 알려줘",
                "적금 추천해줘",
                "이번 달 활동 현황 알려줘"
        );

        emitSmallTalkStream(emitter, response);
        sendEvent(emitter, "replace", response);
        sendEvent(emitter, "suggestions", serialize(suggestions));
        historyService.appendAndSave(userId, userMessage, response, List.of(), suggestions, historyVersion);
        sendEvent(emitter, "done", "");
        emitter.complete();
        return true;
    }

    private void emitSmallTalkStream(SseEmitter emitter, String response) {
        for (String chunk : splitSmallTalkChunks(response)) {
            sendEvent(emitter, "chunk", chunk);
            try {
                Thread.sleep(SMALL_TALK_CHUNK_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private List<String> splitSmallTalkChunks(String response) {
        List<String> chunks = new ArrayList<>();
        String normalized = response.replace("\r\n", "\n").replace('\r', '\n');
        String[] paragraphs = normalized.split("\n\n");

        for (int paragraphIndex = 0; paragraphIndex < paragraphs.length; paragraphIndex++) {
            String paragraph = paragraphs[paragraphIndex].trim();
            if (paragraph.isEmpty()) {
                continue;
            }

            chunks.add(paragraph + (paragraphIndex < paragraphs.length - 1 ? "\n\n" : ""));
        }

        return chunks.isEmpty() ? List.of(response) : chunks;
    }

    private String buildSmallTalkResponse(String normalizedUserMessage) {
        if (containsAny(normalizedUserMessage,
                "안녕", "안뇽", "하이", "ㅎㅇ", "hello", "hi", "반가워", "반갑")) {
            return "안녕하세요 🙂 오늘도 SOLve와 함께 한 걸음씩 실천해봐요.\n\n점수, 추천 활동, 적금 추천, 이번 달 활동 현황처럼 궁금한 내용을 편하게 물어보세요 ✨";
        }

        if (containsAny(normalizedUserMessage,
                "고마워", "감사", "thanks", "thx", "ㄱㅅ")) {
            return "언제든지요 😊 도움이 됐다면 좋겠습니다.\n\n이어서 점수, 활동, 금융 상품 관련해서 궁금한 내용을 편하게 물어보세요.";
        }

        if (containsAny(normalizedUserMessage,
                "좋네", "좋다", "굿", "좋아요", "오케이", "오키", "ㅇㅋ", "알겠어", "알겠어요")) {
            return "좋아요 🙌\n\n이어서 오늘 추천 활동이나 적금 추천처럼 바로 필요한 내용을 물어보시면 이어서 도와드릴게요.";
        }

        return null;
    }

    public String getStaticPolicy() throws IOException {
        if (cachedStaticPolicy != null) {
            return cachedStaticPolicy;
        }
        Resource resource = resourceLoader.getResource("classpath:prompt/chatbot-prompt.txt");
        cachedStaticPolicy = resource.getContentAsString(StandardCharsets.UTF_8);
        return cachedStaticPolicy;
    }

    private String buildSystemPrompt(
            String staticPolicy,
            ChatUserContext context,
            GroundedTurn groundedTurn,
            String latestAssistantMeta
    ) {
        StringBuilder prompt = new StringBuilder(staticPolicy)
                .append("\n\n")
                .append(buildDynamicContext(context));

        if (!groundedTurn.supplementalPrompt().isBlank()) {
            prompt.append("\n\n").append(groundedTurn.supplementalPrompt());
        }

        if (latestAssistantMeta != null && !latestAssistantMeta.isBlank()) {
            prompt.append("\n\n")
                    .append("""
                            === 최근 대화 내부 메타 ===
                            - 아래 메타는 내부 참고용이며 사용자에게 그대로 출력하지 말 것
                            """)
                    .append(latestAssistantMeta);
        }

        return prompt.toString();
    }

    private String buildDynamicContext(ChatUserContext context) {
        String savings = context.getActiveSavings().isEmpty()
                ? "없음"
                : String.join(", ", context.getActiveSavings());

        return String.format("""
                === 현재 사용자 정보 ===
                이름: %s
                사용자 성향: %s
                등급: %s / 총점: %d점
                E점수: %d / S점수: %d / G점수: %d
                보유 포인트: %dP
                이번달 E활동: %d/5점 | S활동: %d/25점 | G활동: %d/10점
                다음 등급까지: %d점 필요
                가입 중인 적금: %s
                대출 현황: %s
                대출 가입 차단 여부: %s
                """,
                context.getName(),
                context.getUserType(),
                context.getGrade(), context.getTotalScore(),
                context.getEScore(), context.getSScore(), context.getGScore(),
                context.getPoint(),
                context.getMonthlyEScore(), context.getMonthlySScore(), context.getMonthlyGScore(),
                context.getNextGradeScore(),
                savings,
                context.isHasActiveLoan() ? "대출 있음" : "없음",
                context.isLoanBlocked() ? "차단됨" : "정상"
        );
    }

    private GroundedTurn buildGroundedTurn(
            Long userId,
            String userMessage,
            String normalizedUserMessage,
            String intent,
            ChatUserContext context
    ) {
        ActivityRecommendResponse activityRecommend = null;
        SavingsRecommendResponse financeRecommend = null;
        String requestedCategory = detectRequestedScoreCategory(normalizedUserMessage);
        boolean popularRequested = isPopularActivityQuestion(normalizedUserMessage);
        UserFeatureDto userFeature = featureExtractor.extract(userId);
        UserActivitySnapshot activitySnapshot = buildUserActivitySnapshot(userId);

        if (shouldLoadActivityRecommendation(intent, normalizedUserMessage, popularRequested)) {
            activityRecommend = requestedCategory != null
                    ? recommendationService.getRecommendationsByCategory(userId, requestedCategory)
                    : recommendCacheService.findActivityRecommend(userId)
                    .orElseGet(() -> recommendationService.getRecommendations(userId));
        }

        if (shouldLoadFinanceRecommendation(intent, normalizedUserMessage)) {
            financeRecommend = financeRecommendService.recommend(loadUser(userId).getLoginId());
        }

        String supplementalPrompt = buildSupplementalPrompt(
                userMessage,
                normalizedUserMessage,
                intent,
                requestedCategory,
                popularRequested,
                activityRecommend,
                financeRecommend,
                context,
                userFeature,
                activitySnapshot
        );

        List<ChatAction> preferredActions = buildPreferredActions(
                userMessage,
                intent,
                requestedCategory,
                popularRequested,
                activityRecommend,
                financeRecommend,
                context,
                userFeature,
                activitySnapshot
        );

        List<String> suggestions = buildGroundedSuggestions(
                intent,
                requestedCategory,
                popularRequested,
                activityRecommend,
                financeRecommend,
                context,
                activitySnapshot,
                userFeature
        );

        return new GroundedTurn(intent, requestedCategory, popularRequested, supplementalPrompt, preferredActions, suggestions);
    }

    private boolean shouldLoadActivityRecommendation(
            String intent,
            String normalizedUserMessage,
            boolean popularRequested
    ) {
        if (INTENT_ACTIVITY.equals(intent) || INTENT_POPULAR.equals(intent) || popularRequested) {
            return true;
        }

        if (INTENT_STATUS.equals(intent) || INTENT_GRADE.equals(intent)) {
            return containsAny(normalizedUserMessage,
                    "추천", "활동", "행동", "올리", "전략", "다음", "인기");
        }

        return false;
    }

    private boolean shouldLoadFinanceRecommendation(String intent, String normalizedUserMessage) {
        if (isLoanQuestion(normalizedUserMessage) && !containsAny(normalizedUserMessage, "적금")) {
            return false;
        }

        if (INTENT_FINANCE.equals(intent)) {
            return true;
        }

        return containsAny(normalizedUserMessage, "적금", "금융 상품", "우대금리", "가입");
    }

    private String buildSupplementalPrompt(
            String userMessage,
            String normalizedUserMessage,
            String intent,
            String requestedCategory,
            boolean popularRequested,
            ActivityRecommendResponse activityRecommend,
            SavingsRecommendResponse financeRecommend,
            ChatUserContext context,
            UserFeatureDto userFeature,
            UserActivitySnapshot activitySnapshot
    ) {
        List<String> sections = new ArrayList<>();

        sections.add("""
                === 이번 질문에 대한 추가 사실 ===
                - 아래 정보가 제공되면 이를 최우선 사실로 사용하고, 임의로 다른 추천 결과를 만들어내지 말 것
                - 추천 엔진 결과는 설명과 해석만 하고, 추천 자체를 바꾸지 말 것
                - 오늘 이미 완료했거나 오늘 재시도가 막힌 활동은 '지금 바로 가능한 활동'으로 추천하지 말 것
                - 이번 달 부족한 활동/카테고리를 묻는 질문이면 '부족한 카테고리'와 '오늘 실제로 가능한 활동'을 반드시 구분해서 답할 것
                - 특정 카테고리가 이번 달 기준으로 부족하더라도 오늘 가능한 활동이 없으면, 부족하다고 설명하되 오늘은 불가능하다고 명확히 말할 것
                - 예: G 점수가 부족해도 오늘 퀴즈를 이미 완료했다면 'G가 부족하지만 오늘은 추가 G 활동이 불가능하다'고 답하고 퀴즈 참여를 권하지 말 것
                - 액션 버튼이 필요하면 본문에서 직접 경로를 쓰지 말고, 추천/설명과 일치하는 페이지 기준으로만 제안할 것
                - 답변은 2~4개의 짧은 문단 또는 짧은 번호 목록으로 정리할 것
                - 문단마다 한 가지 주제만 설명하고, 줄바꿈을 명확히 사용할 것
                """);

        sections.add("현재 질문 의도: " + intent);
        sections.add("현재 질문: " + userMessage);
        sections.add("인기 활동 질문 여부: " + (popularRequested ? "예" : "아니오"));
        sections.add("대출 차단 여부: " + (context.isLoanBlocked() ? "차단됨" : "정상"));
        if (requestedCategory != null) {
            sections.add("사용자가 원하는 활동 카테고리: " + requestedCategory);
            sections.add("카테고리가 지정된 질문이므로 해당 카테고리 활동만 우선 설명할 것");
        }
        if (popularRequested) {
            sections.add("인기 활동 질문이므로 맞춤 추천 Top3 대신 인기 추천 1개만 설명할 것");
        }

        sections.add(buildUserStateFacts(context, userFeature, activitySnapshot));
        sections.add(buildLoanEligibilityFacts(context, normalizedUserMessage));
        sections.add(buildMonthlyGapFacts(context, userFeature, activitySnapshot));
        sections.add(buildQuestionSignalFacts(intent, normalizedUserMessage, requestedCategory, popularRequested));

        if (activityRecommend != null
                && ((activityRecommend.getActivities() != null && !activityRecommend.getActivities().isEmpty())
                || activityRecommend.getPopularActivity() != null)) {
            sections.add(popularRequested
                    ? buildPopularActivityFacts(activityRecommend)
                    : buildActivityRecommendationFacts(activityRecommend, requestedCategory));
        }

        if (financeRecommend != null) {
            sections.add(buildFinanceRecommendationFacts(financeRecommend));
        }

        return String.join("\n", sections);
    }

    private String buildUserStateFacts(
            ChatUserContext context,
            UserFeatureDto userFeature,
            UserActivitySnapshot activitySnapshot
    ) {
        String weakestCategory = switch (userFeature.getWeakestCategory()) {
            case "E" -> "E";
            case "S" -> "S";
            case "G" -> "G";
            default -> "-";
        };

        String environmentStatus = activitySnapshot.environmentStatuses().entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .reduce((left, right) -> left + " | " + right)
                .orElse("없음");

        return String.format("""
                === 사용자 상태 고정 사실 ===
                - 현재 총점 구성: E %d + S %d + G %d = 총 %d
                - 사용자가 E/S/G 점수나 대출 상품 가입 기준을 물으면 G를 활동/상환으로 나누어 설명하지 말고, 반드시 합산된 G 점수로만 설명할 것
                - 이번 달 누적 점수: E %d/5, S %d/25, G %d/10
                - 최근 90일 활동 횟수: E %d회, S %d회, G %d회, 총 %d회
                - 최근 90일 가장 부족한 카테고리: %s
                - 오늘 활동 요약: 퀴즈 %s, 기부 %d건, 봉사 신청 %d건, 가치가게 구매 %d건
                - 오늘 E 활동 상태: %s
                """,
                context.getEScore(),
                context.getSScore(),
                context.getGScore(),
                context.getTotalScore(),
                context.getMonthlyEScore(),
                context.getMonthlySScore(),
                context.getMonthlyGScore(),
                userFeature.getRecentECount(),
                userFeature.getRecentSCount(),
                userFeature.getRecentGCount(),
                userFeature.getTotalActivityCount(),
                weakestCategory,
                activitySnapshot.quizDone() ? "완료" : "미참여",
                activitySnapshot.todayDonationCount(),
                activitySnapshot.todayVolunteerCount(),
                activitySnapshot.todayPurchaseCount(),
                environmentStatus
        ).trim();
    }

    private String buildQuestionSignalFacts(
            String intent,
            String normalizedUserMessage,
            String requestedCategory,
            boolean popularRequested
    ) {
        List<String> lines = new ArrayList<>();
        lines.add("=== 질문 해석 신호 ===");
        lines.add("- 의도: " + intent);
        lines.add("- 인기 활동만 물은 질문: " + (popularRequested ? "예" : "아니오"));
        lines.add("- 특정 카테고리 지정 여부: " + (requestedCategory != null ? requestedCategory : "없음"));
        lines.add("- 부족한 활동/남은 카테고리 분석 질문: "
                + (containsAny(normalizedUserMessage, "부족", "모자라", "남은", "잔여", "채우", "어디가 약") ? "예" : "아니오"));
        lines.add("- 오늘 가능 여부/현황을 함께 고려해야 하는 질문: "
                + (containsAny(normalizedUserMessage, "오늘", "지금", "바로", "현황", "이번 달", "이번달") ? "예" : "아니오"));
        lines.add("- 대출 가입/한도/불가 사유 질문: "
                + (isLoanQuestion(normalizedUserMessage) ? "예" : "아니오"));
        return String.join("\n", lines);
    }

    private String buildLoanEligibilityFacts(ChatUserContext context, String normalizedUserMessage) {
        int totalScore = context.getTotalScore();
        int remainingScore = Math.max(0, LOAN_MIN_ESG_SCORE - totalScore);
        String loanStatus;

        if (context.isLoanBlocked()) {
            loanStatus = "대출 가입 불가: 패널티/ABUSE 등으로 대출 가입 차단 상태";
        } else if (context.isHasActiveLoan()) {
            loanStatus = "대출 가입 불가: 이미 가입 중인 대출 상품이 있음";
        } else if (totalScore < LOAN_MIN_ESG_SCORE) {
            loanStatus = String.format(
                    "현재는 대출 가입 기준에 조금 못 미침: ESG 총점 %d점으로 최소 기준 %d점보다 %d점 부족",
                    totalScore,
                    LOAN_MIN_ESG_SCORE,
                    remainingScore
            );
        } else {
            loanStatus = String.format(
                    "대출 가입 가능권: ESG 총점 %d점으로 최소 기준 %d점 이상",
                    totalScore,
                    LOAN_MIN_ESG_SCORE
            );
        }

        return String.format("""
                === 대출 가입 가능 여부 고정 사실 ===
                - 대출 최소 ESG 점수 기준: %d점 이상
                - 현재 ESG 총점: %d점
                - 대출 차단 여부: %s
                - 활성 대출 보유 여부: %s
                - 최종 판단: %s
                - 주의: '대출 차단 여부 정상'은 ABUSE/패널티 차단이 없다는 뜻일 뿐, 점수 기준을 충족했다는 뜻이 아니다
                - 대출 불가 사유를 묻는 질문이면 위 최종 판단을 우선 사용하고, 적금 추천 사유를 섞지 말 것
                - 점수 미달로 대출 가입이 어려운 경우에는 딱딱하게 "불가"만 말하지 말고, "지금은 기준까지 조금 남았다"는 톤으로 설명할 것
                - 점수 미달로 대출 가입이 어려운 경우에는 점수 구성(E %d점, S %d점, G %d점)만 설명하고, G를 활동/상환으로 나누어 설명하지 말 것
                - 점수 미달로 대출 가입이 어려운 경우에는 부족한 %d점을 E/S/G 활동으로 차근히 올려보자는 짧은 격려 문장을 포함할 것
                - 이 질문에서는 특정 활동명이나 특정 바로가기 추천보다, E/S/G 활동 전반으로 점수를 올릴 수 있다는 방향만 안내할 것
                """,
                LOAN_MIN_ESG_SCORE,
                totalScore,
                context.isLoanBlocked() ? "차단됨" : "정상",
                context.isHasActiveLoan() ? "보유" : "없음",
                loanStatus,
                context.getEScore(),
                context.getSScore(),
                context.getGScore(),
                remainingScore
        ).trim();
    }

    private String buildMonthlyGapFacts(
            ChatUserContext context,
            UserFeatureDto userFeature,
            UserActivitySnapshot activitySnapshot
    ) {
        int remainingE = Math.max(0, 5 - userFeature.getMonthlyEScore());
        int remainingS = Math.max(0, 25 - userFeature.getMonthlySScore());
        int remainingG = Math.max(0, 10 - userFeature.getMonthlyGScore());

        String eAvailability = hasAvailableEAction(userFeature, activitySnapshot)
                ? "오늘 가능한 E 활동 있음"
                : "오늘 가능한 E 활동 없음";
        String sAvailability = hasAvailableSAction(userFeature)
                ? "월 한도 기준 S 활동 여유 있음"
                : "이번 달 S 활동 한도 도달";
        String gAvailability = activitySnapshot.quizDone()
                ? "오늘 퀴즈 이미 완료로 오늘 추가 G 활동 불가"
                : hasAvailableGAction(userFeature, activitySnapshot)
                ? "오늘 퀴즈 참여 가능"
                : "오늘 가능한 G 활동 없음";

        return String.format("""
                === 이번 달 부족분 분석 사실 ===
                - E 남은 점수: %d점 | 상태: %s
                - S 남은 점수: %d점 | 상태: %s
                - G 남은 점수: %d점 | 상태: %s
                - 답변 시 '이번 달 기준 부족한 카테고리'와 '오늘 당장 할 수 있는 활동'을 혼동하지 말 것
                """,
                remainingE, eAvailability,
                remainingS, sAvailability,
                remainingG, gAvailability
        ).trim();
    }

    private String buildActivityRecommendationFacts(ActivityRecommendResponse activityRecommend, String requestedCategory) {
        String title = requestedCategory == null
                ? "활동 추천 엔진 결과"
                : requestedCategory + " 카테고리 활동 추천 엔진 결과";
        StringBuilder facts = new StringBuilder(title).append(":\n");

        int index = 1;
        for (RecommendedActivity activity : activityRecommend.getActivities()) {
            facts.append(String.format(
                    "- 맞춤 추천 %d: %s | 유형=%s | 카테고리=%s | 점수=%d | 포인트=%s | 참여가능=%s | 사유=%s%n",
                    index++,
                    safe(activity.getName()),
                    safe(activity.getActivityType()),
                    safe(activity.getScoreCategory()),
                    activity.getScoreValue(),
                    activity.getPointValue() > 0
                            ? activity.getPointValue() + "P"
                            : String.format("%.0f%% 적립", activity.getPointRate() * 100),
                    Boolean.TRUE.equals(activity.getCanParticipate()) ? "가능" : "제한",
                    safe(activity.getMainReason())
            ));
        }

        RecommendedActivity popularActivity = activityRecommend.getPopularActivity();
        if (popularActivity != null) {
            facts.append(String.format(
                    "- 인기 추천: %s | 유형=%s | 카테고리=%s%n",
                    safe(popularActivity.getName()),
                    safe(popularActivity.getActivityType()),
                    safe(popularActivity.getScoreCategory())
            ));
        }

        if (activityRecommend.getLlmSummary() != null && !activityRecommend.getLlmSummary().isBlank()) {
            facts.append("- 추천 요약: ").append(activityRecommend.getLlmSummary()).append('\n');
        }

        return facts.toString().trim();
    }

    private String buildPopularActivityFacts(ActivityRecommendResponse activityRecommend) {
        RecommendedActivity popularActivity = activityRecommend.getPopularActivity();
        if (popularActivity == null) {
            return """
                    인기 활동 엔진 결과:
                    - 현재 인기 활동 데이터가 없어 맞춤 추천으로 대체하지 말고, 인기 활동을 확인하기 어렵다고 안내할 것
                    """.trim();
        }

        return String.format("""
                인기 활동 엔진 결과:
                - 이 질문은 인기 활동 질문이다. 아래 인기 활동 1개만 설명하고 맞춤 추천 Top3는 대신 설명하지 말 것
                - 인기 추천: %s | 유형=%s | 카테고리=%s | 점수=%d | 포인트=%s | 참여가능=%s | 사유=%s
                """,
                safe(popularActivity.getName()),
                safe(popularActivity.getActivityType()),
                safe(popularActivity.getScoreCategory()),
                popularActivity.getScoreValue(),
                popularActivity.getPointValue() > 0
                        ? popularActivity.getPointValue() + "P"
                        : String.format("%.0f%% 적립", popularActivity.getPointRate() * 100),
                Boolean.TRUE.equals(popularActivity.getCanParticipate()) ? "가능" : "제한",
                safe(popularActivity.getMainReason())
        ).trim();
    }

    private String buildFinanceRecommendationFacts(SavingsRecommendResponse financeRecommend) {
        SavingsRecommendResponse.RecommendItem recommendation = financeRecommend.getRecommendation();
        if (recommendation == null) {
            return """
                    금융 상품 추천 엔진 결과:
                    - 현재 새로 추천할 수 있는 적금 상품이 없습니다
                    - 이미 가입 가능한 적금을 모두 보유했거나 조건상 제외된 상태일 수 있습니다
                    """.trim();
        }

        return String.format("""
                금융 상품 추천 엔진 결과:
                - 추천 상품: %s
                - 예상 최대 금리: %s
                - 매칭 점수: %d
                - 추천 사유: %s
                - 다음 행동: %s
                - 신규 사용자용 추천 여부: %s
                """,
                safe(recommendation.getProductName()),
                safe(recommendation.getExpectedMaxRate()),
                recommendation.getMatchScore(),
                safe(recommendation.getReason()),
                safe(recommendation.getActionable()),
                recommendation.isNewUserRecommend() ? "예" : "아니오"
        ).trim();
    }

    private List<Map<String, String>> buildMessages(
            String systemPrompt,
            List<Map<String, String>> history,
            String userMessage
    ) {
        List<Map<String, String>> messages = new ArrayList<>();

        Map<String, String> system = new HashMap<>();
        system.put("role", "system");
        system.put("content", systemPrompt);
        messages.add(system);

        messages.addAll(history);

        Map<String, String> user = new HashMap<>();
        user.put("role", "user");
        user.put("content", userMessage);
        messages.add(user);

        return messages;
    }

    private List<ChatAction> parseActions(String response) {
        try {
            int start = response.indexOf(ACTIONS_START_MARKER);
            int end = response.indexOf(ACTIONS_END_MARKER);
            if (start == -1 || end == -1) {
                return List.of();
            }

            String json = response.substring(start + ACTIONS_START_MARKER.length(), end).trim();
            Map<?, ?> map = objectMapper.readValue(json, Map.class);
            List<?> rawActions = (List<?>) map.get("actions");
            if (rawActions == null) {
                return List.of();
            }

            List<ChatAction> result = new ArrayList<>();
            for (Object item : rawActions) {
                Map<?, ?> action = (Map<?, ?>) item;
                result.add(ChatAction.builder()
                        .label((String) action.get("label"))
                        .path((String) action.get("path"))
                        .build());
            }
            return result;
        } catch (Exception e) {
            log.warn("actions 파싱 실패: {}", e.getMessage());
            return List.of();
        }
    }

    private String removeActionsBlock(String response) {
        return response.replaceAll("(?s)\\[ACTIONS\\].*?\\[/ACTIONS\\]", "").trim();
    }

    private List<ChatAction> resolveActions(
            String intent,
            List<ChatAction> preferredActions,
            List<ChatAction> parsedActions,
            String userMessage,
            String response
    ) {
        List<ChatAction> groundedActions = limitActions(filterActionsByIntent(
                normalizeActions(preferredActions),
                intent
        ));
        if (!groundedActions.isEmpty()) {
            return groundedActions;
        }

        List<ChatAction> llmActions = limitActions(filterActionsByIntent(
                normalizeActions(parsedActions),
                intent
        ));
        if (!llmActions.isEmpty()) {
            return llmActions;
        }

        String normalizedSource = normalizeKeywordSource(userMessage + " " + response);
        List<ChatAction> fallback = new ArrayList<>();

        if (INTENT_FINANCE.equals(intent)) {
            fallback.addAll(buildFinanceActions(normalizedSource));
        } else if (INTENT_ACTIVITY.equals(intent) || INTENT_POPULAR.equals(intent)) {
            fallback.addAll(buildActivityActions(normalizedSource));
        } else if (INTENT_STATUS.equals(intent)) {
            fallback.addAll(buildStatusActions(normalizedSource));
        } else if (INTENT_GRADE.equals(intent)) {
            fallback.add(ChatAction.builder()
                    .label("내 등급 보러가기")
                    .path("/my/grade")
                    .build());
            if (containsAny(normalizedSource, "활동", "추천", "올리")) {
                fallback.addAll(buildActivityActions(normalizedSource));
            }
        }

        return limitActions(filterActionsByIntent(normalizeActions(fallback), intent));
    }

    private List<String> resolveSuggestions(
            List<String> groundedSuggestions,
            String intent,
            String cleanResponse,
            List<ChatAction> actions
    ) {
        LinkedHashSet<String> suggestions = new LinkedHashSet<>();

        if (groundedSuggestions != null) {
            groundedSuggestions.stream()
                    .filter(suggestion -> suggestion != null && !suggestion.isBlank())
                    .forEach(suggestions::add);
        }

        if (suggestions.size() < MAX_SUGGESTIONS) {
            fallbackSuggestions(intent, cleanResponse, actions).stream()
                    .filter(suggestion -> suggestion != null && !suggestion.isBlank())
                    .forEach(suggestions::add);
        }

        return suggestions.stream()
                .limit(MAX_SUGGESTIONS)
                .toList();
    }

    private String detectIntent(String userMessage, String normalizedSource) {
        if (isPopularActivityQuestion(userMessage) || isPopularActivityQuestion(normalizedSource)) {
            return INTENT_POPULAR;
        }
        if (containsAny(userMessage,
                "적금", "대출", "금융 상품", "금리",
                "적금 추천", "대출 추천", "금융 상품 추천", "상품 추천",
                "우대금리", "가입")) {
            return INTENT_FINANCE;
        }
        if (containsAny(userMessage,
                "이번 달 활동", "이번달 활동", "활동 현황",
                "현재 현황", "월 활동", "점수 현황",
                "부족한 활동", "부족한 카테고리", "뭐가 부족", "어디가 부족",
                "남은 활동", "남은 점수", "잔여 점수")) {
            return INTENT_STATUS;
        }
        if (containsAny(userMessage,
                "활동 추천", "추천 활동", "다음 활동", "다음 행동",
                "e 활동", "s 활동", "g 활동", "기부", "퀴즈", "환경",
                "점수 올리기", "점수 올리는", "쉬운 활동", "할 수 있는 활동",
                "포인트 전략")) {
            return INTENT_ACTIVITY;
        }
        if (containsAny(userMessage, "등급", "점수", "왜 이런 점수", "점수 산정", "등급 올리는")) {
            return INTENT_GRADE;
        }
        if (containsAny(normalizedSource, "적금", "대출", "금리")) {
            return INTENT_FINANCE;
        }
        if (containsAny(normalizedSource, "활동", "퀴즈", "기부", "봉사", "텀블러")) {
            return INTENT_ACTIVITY;
        }
        return INTENT_DEFAULT;
    }

    private List<ChatAction> buildFinanceActions(String normalizedSource) {
        List<ChatAction> actions = new ArrayList<>();

        addIfMentioned(actions, normalizedSource, "그린 스텝업 적금", "그린 스텝업 적금", "/finance/green-step-up-savings");
        addIfMentioned(actions, normalizedSource, "지구 수호대 적금", "지구 수호대 적금", "/finance/earth-guardian-savings");
        addIfMentioned(actions, normalizedSource, "따뜻한 동행 적금", "따뜻한 동행 적금", "/finance/warm-companion-savings");
        addIfMentioned(actions, normalizedSource, "바른 금융 스마트 적금", "바른 금융 스마트 적금", "/finance/smart-finance-savings");
        addIfMentioned(actions, normalizedSource, "esg 마스터 적금", "ESG 마스터 적금", "/finance/esg-master-savings");
        addIfMentioned(actions, normalizedSource, "대출", "대출 보러가기", "/finance/esg-micro-loan");

        if (actions.isEmpty()) {
            actions.add(ChatAction.builder()
                    .label("금융 상품 보러가기")
                    .path("/finance")
                    .build());
        }

        return actions;
    }

    private List<ChatAction> buildActivityActions(String normalizedSource) {
        List<ChatAction> actions = new ArrayList<>();

        if (containsAny(normalizedSource, "텀블러", "공유자전거", "전기차", "e 활동", "환경")) {
            actions.add(ChatAction.builder()
                    .label("환경 활동 보러가기")
                    .path("/activities/environment")
                    .build());
        }

        if (containsAny(normalizedSource, "기부")) {
            actions.add(ChatAction.builder()
                    .label("기부 보러가기")
                    .path("/esg/social/donation")
                    .build());
        }

        if (containsAny(normalizedSource, "가치가게", "사회적 소비", "상품 구매")) {
            actions.add(ChatAction.builder()
                    .label("가치가게 보러가기")
                    .path("/esg/social/store")
                    .build());
        }

        if (containsAny(normalizedSource, "봉사")) {
            actions.add(ChatAction.builder()
                    .label("봉사 보러가기")
                    .path("/esg/social/volunteer")
                    .build());
        }

        if (containsAny(normalizedSource, "퀴즈", "g 활동", "거버넌스")) {
            actions.add(ChatAction.builder()
                    .label("퀴즈 하러가기")
                    .path("/esg/quiz")
                    .build());
        }

        if (actions.isEmpty() && containsAny(normalizedSource, "사회", "s 활동")) {
            actions.add(ChatAction.builder()
                    .label("사회 활동 보러가기")
                    .path("/esg/social/donation")
                    .build());
        }

        return actions;
    }

    private List<ChatAction> buildStatusActions(String normalizedSource) {
        List<ChatAction> actions = new ArrayList<>();

        if (containsAny(normalizedSource, "e 활동: 0", "e 활동은 0", "환경 활동")) {
            actions.add(ChatAction.builder()
                    .label("환경 활동 보러가기")
                    .path("/activities/environment")
                    .build());
        }
        if (containsAny(normalizedSource, "s 활동: 0", "s 활동은 0", "기부")) {
            actions.add(ChatAction.builder()
                    .label("기부 보러가기")
                    .path("/esg/social/donation")
                    .build());
        }
        if (containsAny(normalizedSource, "s 활동: 0", "s 활동은 0", "가치가게")) {
            actions.add(ChatAction.builder()
                    .label("가치가게 보러가기")
                    .path("/esg/social/store")
                    .build());
        }
        if (containsAny(normalizedSource, "s 활동: 0", "s 활동은 0", "봉사")) {
            actions.add(ChatAction.builder()
                    .label("봉사 보러가기")
                    .path("/esg/social/volunteer")
                    .build());
        }
        if (containsAny(normalizedSource, "g 활동: 0", "g 활동은 0", "퀴즈")) {
            actions.add(ChatAction.builder()
                    .label("퀴즈 하러가기")
                    .path("/esg/quiz")
                    .build());
        }

        if (actions.isEmpty()) {
            actions.add(ChatAction.builder()
                    .label("내 등급 보러가기")
                    .path("/my/grade")
                    .build());
        }

        return actions;
    }

    private void addIfMentioned(List<ChatAction> actions, String source, String keyword, String label, String path) {
        if (source.contains(keyword.toLowerCase())) {
            actions.add(ChatAction.builder()
                    .label(label)
                    .path(path)
                    .build());
        }
    }

    private String normalizeAssistantText(String text) {
        String normalized = text
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replace("*", "")
                .replace("**", "")
                .replace("__", "")
                .replace("`", "")
                .replace("# ", "")
                .replace("#", "")
                .replace("> ", "");

        normalized = removeInternalPaths(normalized);
        normalized = normalized.replaceAll("([가-힣])([A-Za-z])", "$1 $2");
        normalized = normalized.replaceAll("([A-Za-z])([가-힣])", "$1 $2");
        normalized = normalized.replaceAll("([가-힣])((?:\\d+(?:/\\d+)?)(?:점|개월|회|만원|원|P|%))", "$1 $2");
        normalized = normalized.replaceAll("([ESG])활동", "$1 활동");
        normalized = normalized.replaceAll("([가-힣0-9]+)(으로|에서|에게|처럼|까지|부터|보다|마다)([가-힣]{2,})", "$1$2 $3");
        normalized = normalized.replaceAll("([가-힣0-9]+)(은|는|이|가|을|를|와|과|의|도|로|에)([가-힣]{2,})", "$1$2 $3");
        normalized = normalized.replaceAll("(다음과 같은)([가-힣])", "$1 $2");
        normalized = normalized.replaceAll("(위 활동들을 통해)([가-힣])", "$1 $2");
        normalized = normalized.replaceAll("(버튼에서)([가-힣])", "$1 $2");
        normalized = normalized.replaceAll("(?m)^참고 액션:.*(?:\n|$)", "");
        normalized = normalized.replaceAll("(?m)^추천 후속 질문:.*(?:\n|$)", "");
        normalized = normalized.replaceAll("[ \\t]+", " ");
        normalized = normalized.replaceAll(" *\n *", "\n");
        normalized = normalized.replaceAll("\n{3,}", "\n\n");
        return normalized.trim();
    }

    private void flushVisibleBuffer(SseEmitter emitter, StringBuilder buffer) {
        if (buffer.length() == 0) {
            return;
        }

        int safeLength = buffer.length() - getTrailingActionMarkerPrefixLength(buffer);
        if (safeLength <= 0) {
            return;
        }

        int flushLength = resolveFlushLength(buffer, safeLength);
        if (flushLength <= 0) {
            return;
        }

        flushVisibleText(emitter, buffer.substring(0, flushLength));
        buffer.delete(0, flushLength);
    }

    private void flushAllVisibleBuffer(SseEmitter emitter, StringBuilder buffer) {
        if (buffer.length() == 0) {
            return;
        }

        flushVisibleText(emitter, buffer.toString());
        buffer.setLength(0);
    }

    private int getTrailingActionMarkerPrefixLength(CharSequence text) {
        int maxPrefixLength = Math.min(text.length(), ACTIONS_START_MARKER.length() - 1);

        for (int prefixLength = maxPrefixLength; prefixLength > 0; prefixLength--) {
            int suffixStart = text.length() - prefixLength;
            String suffix = text.subSequence(suffixStart, text.length()).toString();
            if (ACTIONS_START_MARKER.startsWith(suffix)) {
                return prefixLength;
            }
        }

        return 0;
    }

    private int resolveFlushLength(CharSequence text, int safeLength) {
        if (safeLength <= 0) {
            return 0;
        }

        String safeText = text.subSequence(0, safeLength).toString();
        int sentenceBoundaryIndex = findLastSentenceBoundary(safeText);
        if (sentenceBoundaryIndex >= STREAM_MIN_FLUSH_CHARS - 1) {
            return sentenceBoundaryIndex + 1;
        }

        if (safeLength < STREAM_MIN_FLUSH_CHARS) {
            return 0;
        }

        if (safeLength >= STREAM_FORCE_FLUSH_CHARS) {
            int whitespaceIndex = findLastWhitespaceBefore(safeText, STREAM_FORCE_FLUSH_CHARS);
            if (whitespaceIndex >= STREAM_MIN_FLUSH_CHARS - 1) {
                return whitespaceIndex + 1;
            }
            return STREAM_FORCE_FLUSH_CHARS;
        }

        return 0;
    }

    private int findLastSentenceBoundary(String text) {
        for (int index = text.length() - 1; index >= 0; index--) {
            char current = text.charAt(index);
            if (current == '\n' || current == '.' || current == '!' || current == '?') {
                return index;
            }
        }
        return -1;
    }

    private int findLastWhitespaceBefore(String text, int maxLength) {
        int limit = Math.min(text.length(), maxLength);
        for (int index = limit - 1; index >= 0; index--) {
            if (Character.isWhitespace(text.charAt(index))) {
                return index;
            }
        }
        return -1;
    }

    private void flushVisibleText(SseEmitter emitter, String text) {
        if (text == null || text.isBlank()) {
            return;
        }

        String visibleText = stripMarkdownTokens(text);
        if (visibleText.isBlank()) {
            return;
        }

        sendEvent(emitter, "chunk", visibleText);
    }

    private String stripMarkdownTokens(String text) {
        return removeInternalPaths(text)
                .replace("*", "")
                .replace("**", "")
                .replace("__", "")
                .replace("`", "")
                .replace("#", "");
    }

    private String normalizeKeywordSource(String text) {
        return text == null ? "" : text.toLowerCase();
    }

    private boolean containsAny(String source, String... keywords) {
        for (String keyword : keywords) {
            if (source.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private boolean isLoanQuestion(String normalizedUserMessage) {
        return containsAny(normalizedUserMessage, "대출", "한도", "빌리", "상환");
    }

    private List<ChatAction> buildPreferredActions(
            String userMessage,
            String intent,
            String requestedCategory,
            boolean popularRequested,
            ActivityRecommendResponse activityRecommend,
            SavingsRecommendResponse financeRecommend,
            ChatUserContext context,
            UserFeatureDto userFeature,
            UserActivitySnapshot activitySnapshot
    ) {
        String normalizedUserMessage = normalizeKeywordSource(userMessage);
        LinkedHashSet<ChatAction> actions = new LinkedHashSet<>();

        if (INTENT_FINANCE.equals(intent)) {
            addFinancePreferredActions(actions, financeRecommend, normalizedUserMessage, context);
            return limitActions(new ArrayList<>(actions));
        }

        if (INTENT_ACTIVITY.equals(intent)
                || INTENT_POPULAR.equals(intent)
                || INTENT_STATUS.equals(intent)
                || INTENT_GRADE.equals(intent)) {
            LinkedHashSet<ChatAction> explicitActions = new LinkedHashSet<>();
            addExplicitActivityAction(explicitActions, normalizedUserMessage);

            if (!explicitActions.isEmpty()) {
                actions.addAll(explicitActions);
            } else if (popularRequested) {
                addPopularActivityAction(actions, activityRecommend);
            } else if (INTENT_STATUS.equals(intent)) {
                addStatusPreferredActions(actions, userFeature, activitySnapshot);
            } else if (requestedCategory != null) {
                addCategoryDefaultAction(actions, requestedCategory);
            } else if (INTENT_GRADE.equals(intent)) {
                if (isGradeStrategyQuestion(normalizedUserMessage)) {
                    addGradePreferredActions(actions, activityRecommend, userFeature, activitySnapshot);
                }
            } else if (shouldUseRecommendedActivityActions(normalizedUserMessage, intent)) {
                addRecommendedActivityActions(actions, activityRecommend);
            }

            if (INTENT_STATUS.equals(intent) || INTENT_GRADE.equals(intent)) {
                actions.add(ChatAction.builder()
                        .label("내 등급 보러가기")
                        .path("/my/grade")
                        .build());
            }
        }

        return limitActions(new ArrayList<>(actions));
    }

    private void addStatusPreferredActions(
            LinkedHashSet<ChatAction> actions,
            UserFeatureDto userFeature,
            UserActivitySnapshot activitySnapshot
    ) {
        List<String> categoryPriority = resolveCategoryPriority(userFeature, activitySnapshot);
        for (String category : categoryPriority) {
            addCategoryDefaultAction(actions, category);
            if (actions.size() >= MAX_ACTIONS) {
                break;
            }
        }
    }

    private void addGradePreferredActions(
            LinkedHashSet<ChatAction> actions,
            ActivityRecommendResponse activityRecommend,
            UserFeatureDto userFeature,
            UserActivitySnapshot activitySnapshot
    ) {
        addRecommendedActivityActions(actions, activityRecommend);
        if (actions.isEmpty()) {
            addStatusPreferredActions(actions, userFeature, activitySnapshot);
        }
    }

    private List<String> resolveCategoryPriority(
            UserFeatureDto userFeature,
            UserActivitySnapshot activitySnapshot
    ) {
        record CategoryProgress(String category, double ratio) {
        }

        List<CategoryProgress> progressList = new ArrayList<>();
        if (hasAvailableEAction(userFeature, activitySnapshot)) {
            progressList.add(new CategoryProgress("E", userFeature.getMonthlyEScore() / 5.0));
        }
        if (hasAvailableSAction(userFeature)) {
            progressList.add(new CategoryProgress("S", userFeature.getMonthlySScore() / 25.0));
        }
        if (hasAvailableGAction(userFeature, activitySnapshot)) {
            progressList.add(new CategoryProgress("G", userFeature.getMonthlyGScore() / 10.0));
        }

        progressList.sort(java.util.Comparator.comparingDouble(CategoryProgress::ratio));
        return progressList.stream()
                .map(CategoryProgress::category)
                .toList();
    }

    private boolean hasAvailableEAction(UserFeatureDto userFeature, UserActivitySnapshot activitySnapshot) {
        return userFeature.getMonthlyEScore() < 5
                && activitySnapshot.environmentStatuses().values().stream()
                .anyMatch(status -> status.startsWith("미시도"));
    }

    private boolean hasAvailableSAction(UserFeatureDto userFeature) {
        return userFeature.getMonthlySScore() < 25;
    }

    private boolean hasAvailableGAction(UserFeatureDto userFeature, UserActivitySnapshot activitySnapshot) {
        return userFeature.getMonthlyGScore() < 10 && !activitySnapshot.quizDone();
    }

    private void addPopularActivityAction(
            LinkedHashSet<ChatAction> actions,
            ActivityRecommendResponse activityRecommend
    ) {
        if (activityRecommend == null || activityRecommend.getPopularActivity() == null) {
            return;
        }

        ChatAction mappedAction = mapActivityAction(activityRecommend.getPopularActivity());
        if (mappedAction != null) {
            actions.add(mappedAction);
        }
    }

    private void addCategoryDefaultAction(LinkedHashSet<ChatAction> actions, String requestedCategory) {
        switch (requestedCategory) {
            case "E" -> actions.add(ChatAction.builder()
                    .label("환경 활동 보러가기")
                    .path("/activities/environment")
                    .build());
            case "S" -> actions.add(ChatAction.builder()
                    .label("사회 활동 보러가기")
                    .path("/esg/social/donation")
                    .build());
            case "G" -> actions.add(ChatAction.builder()
                    .label("퀴즈 하러가기")
                    .path("/esg/quiz")
                    .build());
            default -> {
            }
        }
    }

    private boolean shouldUseRecommendedActivityActions(String normalizedUserMessage, String intent) {
        if (INTENT_STATUS.equals(intent) || INTENT_GRADE.equals(intent)) {
            return true;
        }

        return containsAny(normalizedUserMessage,
                "추천", "다음 활동", "다음 행동", "쉬운 활동", "할 수 있는 활동",
                "오늘 할", "무슨 활동", "뭐 하지", "전략");
    }

    private boolean isGradeStrategyQuestion(String normalizedUserMessage) {
        return containsAny(
                normalizedUserMessage,
                "올리", "올리는", "올리기",
                "추천", "활동", "어떻게", "어떤 활동",
                "무슨 활동", "뭐 해야", "전략", "부족"
        );
    }

    private void addFinancePreferredActions(
            LinkedHashSet<ChatAction> actions,
            SavingsRecommendResponse financeRecommend,
            String normalizedUserMessage,
            ChatUserContext context
    ) {
        SavingsRecommendResponse.RecommendItem recommendation =
                financeRecommend != null ? financeRecommend.getRecommendation() : null;
        boolean loanQuestion = isLoanQuestion(normalizedUserMessage);

        if (containsAny(normalizedUserMessage,
                "가입 현황", "가입현황", "보유 적금", "내 적금", "가입한 적금")) {
            actions.add(ChatAction.builder()
                    .label("금융 상품 보러가기")
                    .path("/finance")
                    .build());
            return;
        }

        if (loanQuestion) {
            actions.add(ChatAction.builder()
                    .label("대출 상품 보러가기")
                    .path("/finance/esg-micro-loan")
                    .build());
        } else if (recommendation != null) {
            String detailPath = mapFinancePath(recommendation.getProductName());
            if (detailPath != null) {
                actions.add(ChatAction.builder()
                        .label(recommendation.getProductName() + " 보러가기")
                        .path(detailPath)
                        .build());
            }
        }

        if (containsAny(normalizedUserMessage, "비교", "다른", "목록", "전체")) {
            actions.add(ChatAction.builder()
                    .label("금융 상품 전체 보기")
                    .path("/finance")
                    .build());
        }

        if (actions.isEmpty()) {
            actions.add(ChatAction.builder()
                    .label("금융 상품 보러가기")
                    .path("/finance")
                    .build());
        }
    }

    private void addExplicitActivityAction(LinkedHashSet<ChatAction> actions, String normalizedUserMessage) {
        if (containsAny(normalizedUserMessage, "기부")) {
            actions.add(ChatAction.builder()
                    .label("기부 보러가기")
                    .path("/esg/social/donation")
                    .build());
        }

        if (containsAny(normalizedUserMessage, "봉사")) {
            actions.add(ChatAction.builder()
                    .label("봉사 보러가기")
                    .path("/esg/social/volunteer")
                    .build());
        }

        if (containsAny(normalizedUserMessage, "가치가게", "사회적 소비", "상품 구매")) {
            actions.add(ChatAction.builder()
                    .label("가치가게 보러가기")
                    .path("/esg/social/store")
                    .build());
        }

        if (containsAny(normalizedUserMessage, "퀴즈", "g 활동", "거버넌스")) {
            actions.add(ChatAction.builder()
                    .label("퀴즈 하러가기")
                    .path("/esg/quiz")
                    .build());
        }

        if (containsAny(normalizedUserMessage, "환경", "e 활동", "텀블러", "공유자전거", "전기차")) {
            actions.add(ChatAction.builder()
                    .label("환경 활동 보러가기")
                    .path("/activities/environment")
                    .build());
        }
    }

    private void addRecommendedActivityActions(
            LinkedHashSet<ChatAction> actions,
            ActivityRecommendResponse activityRecommend
    ) {
        if (activityRecommend == null || activityRecommend.getActivities() == null) {
            return;
        }

        for (RecommendedActivity activity : activityRecommend.getActivities()) {
            if (Boolean.FALSE.equals(activity.getCanParticipate())) {
                continue;
            }
            ChatAction mappedAction = mapActivityAction(activity);
            if (mappedAction != null) {
                actions.add(mappedAction);
            }
        }
    }

    private ChatAction mapActivityAction(RecommendedActivity activity) {
        if (activity == null || activity.getActivityType() == null) {
            return null;
        }

        return switch (activity.getActivityType()) {
            case "PHOTO" -> ChatAction.builder()
                    .label("환경 활동 보러가기")
                    .path("/activities/environment")
                    .build();
            case "DONATION" -> ChatAction.builder()
                    .label("기부 보러가기")
                    .path("/esg/social/donation")
                    .build();
            case "VOLUNTEER" -> ChatAction.builder()
                    .label("봉사 보러가기")
                    .path("/esg/social/volunteer")
                    .build();
            case "PURCHASE" -> ChatAction.builder()
                    .label("가치가게 보러가기")
                    .path("/esg/social/store")
                    .build();
            case "QUIZ" -> ChatAction.builder()
                    .label("퀴즈 하러가기")
                    .path("/esg/quiz")
                    .build();
            default -> null;
        };
    }

    private List<String> buildGroundedSuggestions(
            String intent,
            String requestedCategory,
            boolean popularRequested,
            ActivityRecommendResponse activityRecommend,
            SavingsRecommendResponse financeRecommend,
            ChatUserContext context,
            UserActivitySnapshot activitySnapshot,
            UserFeatureDto userFeature
    ) {
        LinkedHashSet<String> suggestions = new LinkedHashSet<>();

        if (INTENT_FINANCE.equals(intent)) {
            SavingsRecommendResponse.RecommendItem recommendation =
                    financeRecommend != null ? financeRecommend.getRecommendation() : null;

            if (recommendation != null) {
                suggestions.add(recommendation.getProductName() + " 추천 이유 다시 설명해줘");
                suggestions.add("다른 적금 상품이랑 비교해줘");
                suggestions.add("우대금리 조건 달성 가능성 알려줘");
            }

            suggestions.add(context.isLoanBlocked()
                    ? "대출이 왜 제한되는지 알려줘"
                    : "대출 상품도 같이 설명해줘");
        } else if (INTENT_POPULAR.equals(intent) || popularRequested) {
            suggestions.add("왜 이 활동이 인기야?");
            suggestions.add(activitySnapshot.quizDone()
                    ? "오늘은 다른 활동 뭐가 좋아?"
                    : "오늘 바로 할 수 있어?");
            suggestions.add("내 맞춤 추천도 알려줘");
            suggestions.add("이번 달 활동 현황도 알려줘");
        } else if (INTENT_ACTIVITY.equals(intent)) {
            addActivitySuggestions(suggestions, requestedCategory, activityRecommend, activitySnapshot, userFeature);
            if ("E".equals(requestedCategory)) {
                suggestions.add(hasAvailableEAction(userFeature, activitySnapshot)
                        ? "오늘 할 수 있는 E 활동 더 추천해줘"
                        : "오늘 E 활동이 왜 막혔는지 알려줘");
                suggestions.add("환경 활동 점수 규칙 알려줘");
            } else if ("S".equals(requestedCategory)) {
                suggestions.add("S 활동 중 가장 쉬운 걸 추천해줘");
                suggestions.add("기부와 봉사 중 뭐가 더 좋아?");
            } else if ("G".equals(requestedCategory)) {
                suggestions.add(activitySnapshot.quizDone()
                        ? "오늘은 다른 활동 뭐가 좋아?"
                        : "오늘 퀴즈 바로 하러가고 싶어");
                suggestions.add("G 점수 규칙 알려줘");
            } else {
                suggestions.add("이번 달 부족한 활동이 뭐야?");
                suggestions.add("점수 올리기 쉬운 활동으로 다시 추천해줘");
            }
        } else if (INTENT_STATUS.equals(intent)) {
            addStatusSuggestions(suggestions);
        } else if (INTENT_GRADE.equals(intent)) {
            addGradeSuggestions(suggestions);
        } else {
            suggestions.add("오늘 추천 활동 다시 알려줘");
            suggestions.add("적금 추천해줘");
            suggestions.add("이번 달 활동 현황 알려줘");
            suggestions.add("등급 올리는 방법 알려줘");
        }

        return suggestions.stream()
                .filter(suggestion -> suggestion != null && !suggestion.isBlank())
                .limit(MAX_SUGGESTIONS)
                .toList();
    }

    private void addActivitySuggestions(
            LinkedHashSet<String> suggestions,
            String requestedCategory,
            ActivityRecommendResponse activityRecommend,
            UserActivitySnapshot activitySnapshot,
            UserFeatureDto userFeature
    ) {
        if (activityRecommend == null || activityRecommend.getActivities() == null || activityRecommend.getActivities().isEmpty()) {
            suggestions.add("오늘 바로 할 수 있는 활동 추천해줘");
            suggestions.add("가장 쉬운 활동으로 추천해줘");
            return;
        }

        boolean hasQuiz = activityRecommend.getActivities().stream()
                .anyMatch(activity -> "QUIZ".equals(activity.getActivityType()) && !Boolean.FALSE.equals(activity.getCanParticipate()));
        boolean hasDonation = activityRecommend.getActivities().stream()
                .anyMatch(activity -> "DONATION".equals(activity.getActivityType()) && !Boolean.FALSE.equals(activity.getCanParticipate()));
        boolean hasVolunteer = activityRecommend.getActivities().stream()
                .anyMatch(activity -> "VOLUNTEER".equals(activity.getActivityType()) && !Boolean.FALSE.equals(activity.getCanParticipate()));
        boolean hasPhoto = activityRecommend.getActivities().stream()
                .anyMatch(activity -> "PHOTO".equals(activity.getActivityType()) && !Boolean.FALSE.equals(activity.getCanParticipate()));

        if (requestedCategory == null) {
            suggestions.add("오늘 바로 할 수 있는 활동 추천해줘");
            suggestions.add("점수 올리기 쉬운 활동 추천해줘");
        }

        if (hasPhoto && hasAvailableEAction(userFeature, activitySnapshot)) {
            suggestions.add("오늘 할 수 있는 E 활동 알려줘");
        }
        if (hasDonation || hasVolunteer) {
            suggestions.add("S 활동 중에서 지금 하기 쉬운 걸 알려줘");
        }
        if (hasQuiz && !activitySnapshot.quizDone()) {
            suggestions.add("퀴즈로 점수 얼마나 올릴 수 있어?");
        }
    }

    private void addStatusSuggestions(LinkedHashSet<String> suggestions) {
        suggestions.add("이번 달 남은 한도 알려줘");
        suggestions.add("이번 달 부족한 활동이 뭐야?");
        suggestions.add("오늘 추천 활동 알려줘");
        suggestions.add("등급 올리기 좋은 활동 추천해줘");
    }

    private void addGradeSuggestions(LinkedHashSet<String> suggestions) {
        suggestions.add("왜 현재 점수가 이렇게 계산됐는지 알려줘");
        suggestions.add("이번 달 활동 현황 알려줘");
        suggestions.add("등급 올리기 좋은 활동 추천해줘");
        suggestions.add("오늘 추천 활동 알려줘");
    }

    private List<String> fallbackSuggestions(String intent, String cleanResponse, List<ChatAction> actions) {
        LinkedHashSet<String> suggestions = new LinkedHashSet<>();

        if (INTENT_FINANCE.equals(intent)) {
            suggestions.add("다른 적금 상품도 비교해줘");
            suggestions.add("금리 조건 다시 정리해줘");
            suggestions.add("나한테 가장 유리한 적금은?");
        } else if (INTENT_POPULAR.equals(intent)) {
            suggestions.add("왜 이 활동이 인기야?");
            suggestions.add("내 맞춤 추천도 알려줘");
            suggestions.add("오늘 바로 할 수 있는 활동은?");
        } else if (INTENT_ACTIVITY.equals(intent)) {
            suggestions.add("오늘 바로 할 수 있는 활동은?");
            suggestions.add("점수 올리기 쉬운 활동 추천해줘");
            suggestions.add("이번 달 활동 현황 알려줘");
        } else if (INTENT_STATUS.equals(intent)) {
            suggestions.add("이번 달 남은 한도 알려줘");
            suggestions.add("오늘 추천 활동 알려줘");
            suggestions.add("등급 올리기 좋은 활동 추천해줘");
        } else if (INTENT_GRADE.equals(intent)) {
            suggestions.add("왜 현재 점수가 이렇게 계산됐어?");
            suggestions.add("이번 달 활동 현황 알려줘");
            suggestions.add("오늘 추천 활동 알려줘");
        } else {
            suggestions.add("내 점수/등급 알려줘");
            suggestions.add("오늘 추천 활동");
            suggestions.add("적금 추천해줘");
        }

        if (actions.stream().anyMatch(action -> "/esg/quiz".equals(action.getPath()))
                || cleanResponse.contains("퀴즈")) {
            suggestions.add("퀴즈 점수는 얼마야?");
        }

        return suggestions.stream()
                .limit(MAX_SUGGESTIONS)
                .toList();
    }

    private List<ChatAction> deduplicateActions(List<ChatAction> actions) {
        LinkedHashSet<String> seenPaths = new LinkedHashSet<>();
        List<ChatAction> deduplicated = new ArrayList<>();

        for (ChatAction action : actions) {
            if (action == null || action.getPath() == null || seenPaths.contains(action.getPath())) {
                continue;
            }
            seenPaths.add(action.getPath());
            deduplicated.add(action);
        }

        return deduplicated;
    }

    private List<ChatAction> normalizeActions(List<ChatAction> actions) {
        return actions.stream()
                .filter(action -> action != null && action.getPath() != null)
                .map(this::normalizeAction)
                .toList();
    }

    private ChatAction normalizeAction(ChatAction action) {
        String normalizedPath = normalizeActionPath(action.getPath());
        String label = action.getLabel();

        if (label == null || label.isBlank()) {
            label = defaultActionLabel(normalizedPath);
        }

        return ChatAction.builder()
                .label(label)
                .path(normalizedPath)
                .build();
    }

    private String normalizeActionPath(String path) {
        if ("/esg/social".equals(path) || "/activities/social".equals(path)) {
            return "/esg/social/donation";
        }

        if ("/activities/governance".equals(path)) {
            return "/esg/quiz";
        }

        return path;
    }

    private String defaultActionLabel(String path) {
        return switch (path) {
            case "/finance" -> "금융 상품 보러가기";
            case "/finance/green-step-up-savings" -> "그린 스텝업 적금 보러가기";
            case "/finance/earth-guardian-savings" -> "지구 수호대 적금 보러가기";
            case "/finance/warm-companion-savings" -> "따뜻한 동행 적금 보러가기";
            case "/finance/smart-finance-savings" -> "바른 금융 스마트 적금 보러가기";
            case "/finance/esg-master-savings" -> "ESG 마스터 적금 보러가기";
            case "/finance/esg-micro-loan" -> "대출 상품 보러가기";
            case "/activities/environment" -> "환경 활동 보러가기";
            case "/esg/social/donation" -> "기부 보러가기";
            case "/esg/social/store" -> "가치가게 보러가기";
            case "/esg/social/volunteer" -> "봉사 보러가기";
            case "/esg/quiz" -> "퀴즈 하러가기";
            case "/my" -> "내 정보 보러가기";
            case "/my/grade" -> "내 등급 보러가기";
            default -> "바로가기";
        };
    }

    private List<ChatAction> filterActionsByIntent(List<ChatAction> actions, String intent) {
        if (actions.isEmpty()) {
            return List.of();
        }

        return actions.stream()
                .filter(action -> isAllowedActionPath(action.getPath()))
                .filter(action -> isIntentCompatible(intent, action.getPath()))
                .toList();
    }

    private boolean isIntentCompatible(String intent, String path) {
        if (INTENT_FINANCE.equals(intent)) {
            return path.startsWith("/finance");
        }

        if (INTENT_ACTIVITY.equals(intent) || INTENT_POPULAR.equals(intent)) {
            return ACTIVITY_ACTION_PATHS.contains(path);
        }

        if (INTENT_STATUS.equals(intent) || INTENT_GRADE.equals(intent)) {
            return STATUS_ACTION_PATHS.contains(path);
        }

        return true;
    }

    private boolean isAllowedActionPath(String path) {
        return path != null && ALLOWED_ACTION_PATHS.contains(path);
    }

    private List<ChatAction> limitActions(List<ChatAction> actions) {
        return deduplicateActions(actions).stream()
                .limit(MAX_ACTIONS)
                .toList();
    }

    private String removeInternalPaths(String text) {
        return text
                .replaceAll("\\((/(finance|activities|esg|my)[^)]*)\\)", "")
                .replaceAll("(?<![A-Za-z0-9])/(finance|activities|esg|my)\\S*", "");
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "[]";
        }
    }

    private void sendEvent(SseEmitter emitter, String eventName, String data) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
        } catch (IOException | IllegalStateException e) {
            log.debug("SSE 이벤트 전송 실패 (클라이언트 연결 종료) event={}", eventName);
        }
    }

    private User loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("사용자를 찾을 수 없습니다."));
    }

    private String detectRequestedScoreCategory(String normalizedUserMessage) {
        if (containsAny(normalizedUserMessage,
                "e 활동", "e활동",
                "환경 활동", "환경활동", "환경 추천", "환경추천",
                "친환경 활동", "친환경활동",
                "텀블러", "공유자전거", "전기차")) {
            return "E";
        }
        if (containsAny(normalizedUserMessage,
                "g 활동", "g활동",
                "거버넌스 활동", "거버넌스활동",
                "퀴즈 추천", "퀴즈추천",
                "퀴즈", "금융 퀴즈", "금융퀴즈", "거버넌스")) {
            return "G";
        }
        if (containsAny(normalizedUserMessage,
                "s 활동", "s활동",
                "사회 활동", "사회활동", "사회 추천", "사회추천",
                "기부 추천", "기부추천",
                "봉사 추천", "봉사추천",
                "가치가게")) {
            return "S";
        }
        return null;
    }

    private boolean isPopularActivityQuestion(String normalizedUserMessage) {
        return containsAny(normalizedUserMessage,
                "인기 활동", "인기활동",
                "인기 추천", "인기추천",
                "오늘 인기", "요즘 인기",
                "많이 하는 활동", "많이한 활동", "트렌드");
    }

    private UserActivitySnapshot buildUserActivitySnapshot(Long userId) {
        User user = loadUser(userId);
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        Map<String, UserEnvironmentActivity> latestAttempts = new HashMap<>();
        for (UserEnvironmentActivity activity : userEnvironmentActivityRepository.findAllByUserAndCreatedAtBetween(
                user,
                startOfDay,
                endOfDay
        )) {
            String activityName = activity.getActivity().getName();
            UserEnvironmentActivity currentLatest = latestAttempts.get(activityName);
            if (currentLatest == null || activity.getCreatedAt().isAfter(currentLatest.getCreatedAt())) {
                latestAttempts.put(activityName, activity);
            }
        }

        Map<String, String> environmentStatuses = new LinkedHashMap<>();
        for (String activityName : ENVIRONMENT_ACTIVITY_NAMES) {
            UserEnvironmentActivity attempt = latestAttempts.get(activityName);
            environmentStatuses.put(activityName, describeEnvironmentStatus(attempt));
        }

        return new UserActivitySnapshot(
                userQuizRepository.countToday(userId, startOfDay) > 0,
                userDonationRepository.countTodayByUser(userId, startOfDay),
                userVolunteerRepository.countTodayByUser(userId, startOfDay),
                userEcoProductRepository.countTodayByUser(userId, startOfDay),
                environmentStatuses
        );
    }

    private String describeEnvironmentStatus(UserEnvironmentActivity attempt) {
        if (attempt == null) {
            return "미시도";
        }

        if (Boolean.TRUE.equals(attempt.getIsApproved())) {
            return "오늘 승인 완료";
        }

        String reason = safe(attempt.getAdminComment());
        if (!"-".equals(reason)) {
            return "오늘 시도 실패, 오늘 재시도 불가, 사유=" + reason;
        }
        return "오늘 시도 실패, 오늘 재시도 불가";
    }

    private String mapFinancePath(String productName) {
        if (productName == null) {
            return null;
        }

        return switch (productName) {
            case "그린 스텝업 적금" -> "/finance/green-step-up-savings";
            case "지구 수호대 적금" -> "/finance/earth-guardian-savings";
            case "따뜻한 동행 적금" -> "/finance/warm-companion-savings";
            case "바른 금융 스마트 적금" -> "/finance/smart-finance-savings";
            case "ESG 마스터 적금" -> "/finance/esg-master-savings";
            case "ESG 소액대출" -> "/finance/esg-micro-loan";
            default -> "/finance";
        };
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private record GroundedTurn(
            String intent,
            String requestedCategory,
            boolean popularRequested,
            String supplementalPrompt,
            List<ChatAction> preferredActions,
            List<String> suggestions
    ) {
    }

    private record UserActivitySnapshot(
            boolean quizDone,
            long todayDonationCount,
            long todayVolunteerCount,
            long todayPurchaseCount,
            Map<String, String> environmentStatuses
    ) {
    }
}
