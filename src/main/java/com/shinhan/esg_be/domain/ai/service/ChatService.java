package com.shinhan.esg_be.domain.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shinhan.esg_be.domain.ai.dto.ChatAction;
import com.shinhan.esg_be.domain.ai.dto.ChatUserContext;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final String ACTIONS_START_MARKER = "[ACTIONS]";
    private static final String ACTIONS_END_MARKER = "[/ACTIONS]";
    private static final int STREAM_HOLD_BACK_LENGTH = 16;
    private static final String INTENT_FINANCE = "finance";
    private static final String INTENT_ACTIVITY = "activity";
    private static final String INTENT_STATUS = "status";
    private static final String INTENT_GRADE = "grade";
    private static final String INTENT_DEFAULT = "default";

    private final LLMClient llmClient;
    private final ChatContextService contextService;
    private final ChatHistoryService historyService;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    @Qualifier("chatExecutor")
    private final ThreadPoolTaskExecutor chatExecutor;

    private String cachedStaticPolicy = null;

    public SseEmitter streamChat(Long userId, String userMessage) {
        SseEmitter emitter = new SseEmitter(60_000L);

        chatExecutor.execute(() -> {
            try {
                String staticPolicy = getStaticPolicy();
                ChatUserContext context = contextService.getOrLoad(userId);
                String dynamicContext = buildDynamicContext(context);

                String systemPrompt = staticPolicy + "\n\n" + dynamicContext;
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
                                        visibleBuffer.substring(0, actionStartIndex),
                                        true
                                );
                                visibleBuffer.setLength(0);
                                actionBlockStarted[0] = true;
                                return;
                            }

                            flushVisibleBuffer(emitter, visibleBuffer, false);
                        },
                        () -> {
                            String response = fullResponse.toString();
                            List<ChatAction> actions = enrichActions(
                                    parseActions(response),
                                    userMessage,
                                    response
                            );
                            String cleanResponse = normalizeAssistantText(removeActionsBlock(response));

                            if (!actionBlockStarted[0]) {
                                flushVisibleBuffer(emitter, visibleBuffer, true);
                            }

                            if (!actions.isEmpty()) {
                                sendEvent(emitter, "actions", serializeActions(actions));
                            }

                            sendEvent(emitter, "done", "");
                            emitter.complete();

                            historyService.appendAndSave(userId, userMessage, cleanResponse, actions);
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

    public String getStaticPolicy() throws IOException {
        if (cachedStaticPolicy != null) {
            return cachedStaticPolicy;
        }
        Resource resource = resourceLoader.getResource("classpath:prompt/chatbot-prompt.txt");
        cachedStaticPolicy = resource.getContentAsString(StandardCharsets.UTF_8);
        return cachedStaticPolicy;
    }

    private String buildDynamicContext(ChatUserContext context) {
        String savings = context.getActiveSavings().isEmpty()
                ? "없음"
                : String.join(", ", context.getActiveSavings());

        return String.format("""
                === 현재 사용자 정보 ===
                이름: %s
                등급: %s / 총점: %d점
                E점수: %d / S점수: %d / G점수: %d
                보유 포인트: %dP
                이번달 E활동: %d/5점 | S활동: %d/25점 | G활동: %d/10점
                다음 등급까지: %d점 필요
                가입 중인 적금: %s
                대출 현황: %s
                """,
                context.getName(),
                context.getGrade(), context.getTotalScore(),
                context.getEScore(), context.getSScore(), context.getGScore(),
                context.getPoint(),
                context.getMonthlyEScore(), context.getMonthlySScore(), context.getMonthlyGScore(),
                context.getNextGradeScore(),
                savings,
                context.isHasActiveLoan() ? "대출 있음" : "없음"
        );
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

    private List<ChatAction> enrichActions(
            List<ChatAction> parsedActions,
            String userMessage,
            String response
    ) {
        String normalizedUserMessage = userMessage.toLowerCase();
        String normalizedSource = (userMessage + " " + response).toLowerCase();
        String intent = detectIntent(normalizedUserMessage, normalizedSource);

        List<ChatAction> filteredParsedActions = filterActionsByIntent(normalizeActions(parsedActions), intent);
        if (!filteredParsedActions.isEmpty()) {
            return deduplicateActions(filteredParsedActions);
        }

        List<ChatAction> actions = new ArrayList<>();

        if (INTENT_FINANCE.equals(intent)) {
            actions.addAll(buildFinanceActions(normalizedSource));
        } else if (INTENT_ACTIVITY.equals(intent)) {
            actions.addAll(buildActivityActions(normalizedSource));
        } else if (INTENT_STATUS.equals(intent)) {
            actions.addAll(buildStatusActions(normalizedSource));
        } else if (INTENT_GRADE.equals(intent)) {
            actions.add(ChatAction.builder()
                    .label("내 등급 보러가기")
                    .path("/my/grade")
                    .build());
            if (containsAny(normalizedSource, "활동", "추천", "올리")) {
                actions.addAll(buildActivityActions(normalizedSource));
            }
        } else {
            actions.add(ChatAction.builder()
                    .label("챗봇에서 다시 물어보기")
                    .path("/chatbot")
                    .build());
        }

        return deduplicateActions(normalizeActions(actions));
    }

    private String detectIntent(String userMessage, String normalizedSource) {
        if (containsAny(userMessage,
                "적금", "대출", "금융 상품", "금리",
                "적금 추천", "대출 추천", "금융 상품 추천", "상품 추천")) {
            return INTENT_FINANCE;
        }
        if (containsAny(userMessage, "이번 달 활동", "이번달 활동", "활동 현황")) {
            return INTENT_STATUS;
        }
        if (containsAny(userMessage,
                "활동 추천", "추천 활동", "e 활동", "s 활동", "g 활동",
                "기부", "퀴즈", "환경", "점수 올리기", "점수 올리는", "쉬운 활동", "할 수 있는 활동")) {
            return INTENT_ACTIVITY;
        }
        if (containsAny(userMessage, "등급", "점수")) {
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
        addIfMentioned(actions, normalizedSource, "ESG 마스터 적금", "ESG 마스터 적금", "/finance/esg-master-savings");

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
        normalized = normalized.replaceAll("(?<!\\n)(\\d+\\.)(?=\\s*[A-Za-z가-힣])", "\n$1");
        normalized = normalized.replaceAll("(?<!\\n)(-)(?=\\s*[A-Za-z가-힣])", "\n$1");
        normalized = normalized.replaceAll("(^|\\n)(\\d+)\\.\\s*([A-Za-z가-힣])", "$1$2. $3");
        normalized = normalized.replaceAll("([가-힣])([A-Za-z])", "$1 $2");
        normalized = normalized.replaceAll("([A-Za-z])([가-힣])", "$1 $2");
        normalized = normalized.replaceAll("([가-힣])((?:\\d+(?:/\\d+)?)(?:점|개월|회|만원|원|P|%))", "$1 $2");
        normalized = normalized.replaceAll("([ESG])활동", "$1 활동");
        normalized = normalized.replaceAll("([가-힣0-9]+)(으로|에서|에게|처럼|까지|부터|보다|마다)([가-힣]{2,})", "$1$2 $3");
        normalized = normalized.replaceAll("([가-힣0-9]+)(은|는|이|가|을|를|와|과|의|도|로|에)([가-힣]{2,})", "$1$2 $3");
        normalized = normalized.replaceAll("(다음과 같은)([가-힣])", "$1 $2");
        normalized = normalized.replaceAll("(위 활동들을 통해)([가-힣])", "$1 $2");
        normalized = normalized.replaceAll("(버튼에서)([가-힣])", "$1 $2");
        normalized = normalized.replaceAll("[ \\t]+", " ");
        normalized = normalized.replaceAll(" *\n *", "\n");
        normalized = normalized.replaceAll("\n{3,}", "\n\n");
        return normalized.trim();
    }

    private void flushVisibleBuffer(SseEmitter emitter, StringBuilder buffer, boolean force) {
        if (buffer.length() == 0) {
            return;
        }

        if (!force && buffer.length() <= STREAM_HOLD_BACK_LENGTH) {
            return;
        }

        int flushLength = force
                ? buffer.length()
                : buffer.length() - STREAM_HOLD_BACK_LENGTH;

        if (flushLength <= 0) {
            return;
        }

        String chunk = buffer.substring(0, flushLength);
        flushVisibleText(emitter, chunk, force);
        buffer.delete(0, flushLength);
    }

    private void flushVisibleText(SseEmitter emitter, String text, boolean finalChunk) {
        if (text == null || text.isBlank()) {
            return;
        }

        String visibleText = finalChunk ? normalizeAssistantText(text) : stripMarkdownTokens(text);
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

    private boolean containsAny(String source, String... keywords) {
        for (String keyword : keywords) {
            if (source.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private List<ChatAction> deduplicateActions(List<ChatAction> actions) {
        List<ChatAction> deduplicated = new ArrayList<>();
        for (ChatAction action : actions) {
            boolean exists = deduplicated.stream()
                    .anyMatch(existing -> existing.getPath().equals(action.getPath()));
            if (!exists) {
                deduplicated.add(action);
            }
        }
        return deduplicated;
    }

    private List<ChatAction> normalizeActions(List<ChatAction> actions) {
        return actions.stream()
                .map(this::normalizeAction)
                .toList();
    }

    private ChatAction normalizeAction(ChatAction action) {
        String normalizedPath = normalizeActionPath(action.getPath());
        if ((action.getPath() == null && normalizedPath == null)
                || (action.getPath() != null && action.getPath().equals(normalizedPath))) {
            return action;
        }

        return ChatAction.builder()
                .label(action.getLabel())
                .path(normalizedPath)
                .build();
    }

    private String normalizeActionPath(String path) {
        if ("/esg/social".equals(path) || "/activities/social".equals(path)) {
            return "/esg/social/donation";
        }

        return path;
    }

    private List<ChatAction> filterActionsByIntent(List<ChatAction> actions, String intent) {
        if (actions.isEmpty()) {
            return List.of();
        }

        List<ChatAction> filtered = new ArrayList<>();
        for (ChatAction action : actions) {
            String path = action.getPath();
            if (path == null || path.isBlank()) {
                continue;
            }

            if (INTENT_FINANCE.equals(intent) && path.startsWith("/finance")) {
                filtered.add(action);
                continue;
            }

            if (INTENT_ACTIVITY.equals(intent)
                    && (path.startsWith("/activities")
                    || path.startsWith("/esg/social")
                    || "/esg/quiz".equals(path))) {
                filtered.add(action);
                continue;
            }

            if (INTENT_STATUS.equals(intent)
                    && (path.startsWith("/activities")
                    || path.startsWith("/esg/social")
                    || "/esg/quiz".equals(path)
                    || "/my/grade".equals(path))) {
                filtered.add(action);
                continue;
            }

            if (INTENT_GRADE.equals(intent)
                    && ("/my".equals(path) || "/my/grade".equals(path))) {
                filtered.add(action);
                continue;
            }

            if (INTENT_DEFAULT.equals(intent) && "/chatbot".equals(path)) {
                filtered.add(action);
            }
        }

        return filtered;
    }

    private String removeInternalPaths(String text) {
        return text
                .replaceAll("\\((/(finance|activities|esg|my)[^)]*)\\)", "")
                .replaceAll("(?<![A-Za-z0-9])/(finance|activities|esg|my)\\S*", "");
    }

    private String serializeActions(List<ChatAction> actions) {
        try {
            return objectMapper.writeValueAsString(actions);
        } catch (Exception e) {
            return "[]";
        }
    }

    private void sendEvent(SseEmitter emitter, String eventName, String data) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
        } catch (IOException e) {
            log.debug("SSE 이벤트 전송 실패 (클라이언트 연결 종료) event={}", eventName);
        }
    }
}
