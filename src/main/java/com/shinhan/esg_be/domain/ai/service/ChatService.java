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
                List<Map<String, String>> history = historyService.getHistory(userId);
                List<Map<String, String>> messages = buildMessages(systemPrompt, history, userMessage);

                StringBuilder fullResponse = new StringBuilder();
                StringBuilder streamBuffer = new StringBuilder();
                llmClient.stream(
                        messages,
                        chunk -> {
                            fullResponse.append(chunk);
                            streamBuffer.append(chunk);
                            flushBufferedChunk(emitter, streamBuffer, false);
                        },
                        () -> {
                            flushBufferedChunk(emitter, streamBuffer, true);

                            String response = fullResponse.toString();
                            List<ChatAction> actions = parseActions(response);
                            String cleanResponse = sanitizeAssistantText(removeActionsBlock(response));

                            if (!actions.isEmpty()) {
                                sendEvent(emitter, "actions", serializeActions(actions));
                            }

                            sendEvent(emitter, "done", "");
                            emitter.complete();

                            historyService.appendAndSave(userId, userMessage, cleanResponse);
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

    private String getStaticPolicy() throws IOException {
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
            int start = response.indexOf("[ACTIONS]");
            int end = response.indexOf("[/ACTIONS]");
            if (start == -1 || end == -1) {
                return List.of();
            }

            String json = response.substring(start + 9, end).trim();
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

    private String sanitizeAssistantText(String text) {
        String sanitized = text
                .replace("**", "")
                .replace("__", "")
                .replace("`", "")
                .replace("#", "");
        return sanitized.replaceAll("[ \\t]{2,}", " ").trim();
    }

    private void flushBufferedChunk(SseEmitter emitter, StringBuilder buffer, boolean force) {
        if (buffer.length() == 0) {
            return;
        }

        if (!force && !shouldFlush(buffer)) {
            return;
        }

        int flushLength = buffer.length();
        if (!force) {
            while (flushLength > 0 && Character.isWhitespace(buffer.charAt(flushLength - 1))) {
                flushLength--;
            }
            if (flushLength == 0) {
                return;
            }
        }

        String chunk = buffer.substring(0, flushLength);
        String sanitizedChunk = sanitizeAssistantText(chunk);
        if (!sanitizedChunk.isEmpty()) {
            sendEvent(emitter, "chunk", sanitizedChunk);
        }
        buffer.delete(0, flushLength);
    }

    private boolean shouldFlush(StringBuilder buffer) {
        if (buffer.length() >= 24) {
            return true;
        }
        char last = buffer.charAt(buffer.length() - 1);
        return last == '.' || last == '!' || last == '?' || last == '\n'
                || last == '。' || last == '！' || last == '？';
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
