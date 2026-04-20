package com.shinhan.esg_be.domain.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shinhan.esg_be.domain.ai.dto.ChatAction;
import com.shinhan.esg_be.domain.ai.dto.ChatHistoryMessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatHistoryService {

    private static final String KEY_PREFIX = "chat:history:";
    private static final String VERSION_KEY_PREFIX = "chat:history:version:";
    private static final Duration TTL = Duration.ofHours(2);
    private static final int MAX_UI_MESSAGES = 30;
    private static final int MAX_PROMPT_MESSAGES = 10;

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public List<Map<String, String>> getPromptHistory(Long userId) {
        List<ChatHistoryMessageResponse> fullHistory = getUiHistory(userId);
        int startIndex = Math.max(fullHistory.size() - MAX_PROMPT_MESSAGES, 0);
        return fullHistory.subList(startIndex, fullHistory.size()).stream()
                .map(message -> Map.of(
                        "role", message.getRole(),
                        "content", message.getContent()
                ))
                .toList();
    }

    public String getLatestAssistantMetaPrompt(Long userId) {
        List<ChatHistoryMessageResponse> history = getUiHistory(userId);

        for (int index = history.size() - 1; index >= 0; index--) {
            ChatHistoryMessageResponse message = history.get(index);
            if (!"assistant".equals(message.getRole())) {
                continue;
            }

            List<String> parts = new ArrayList<>();

            if (message.getActions() != null && !message.getActions().isEmpty()) {
                String actionSummary = message.getActions().stream()
                        .map(ChatAction::getLabel)
                        .distinct()
                        .reduce((left, right) -> left + ", " + right)
                        .orElse("");
                if (!actionSummary.isBlank()) {
                    parts.add("직전 액션: " + actionSummary);
                }
            }

            if (message.getSuggestions() != null && !message.getSuggestions().isEmpty()) {
                String suggestionSummary = message.getSuggestions().stream()
                        .distinct()
                        .reduce((left, right) -> left + " | " + right)
                        .orElse("");
                if (!suggestionSummary.isBlank()) {
                    parts.add("직전 추천 질문: " + suggestionSummary);
                }
            }

            return String.join("\n", parts);
        }

        return "";
    }

    public List<ChatHistoryMessageResponse> getUiHistory(Long userId) {
        String key = KEY_PREFIX + userId;
        try {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (json == null) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(json, new TypeReference<List<ChatHistoryMessageResponse>>() {
            });
        } catch (Exception e) {
            log.warn("대화 이력 조회 실패 userId={}", userId, e);
            return new ArrayList<>();
        }
    }

    public long getHistoryVersion(Long userId) {
        try {
            String rawVersion = stringRedisTemplate.opsForValue().get(VERSION_KEY_PREFIX + userId);
            if (rawVersion == null || rawVersion.isBlank()) {
                return 0L;
            }
            return Long.parseLong(rawVersion);
        } catch (Exception e) {
            log.warn("대화 이력 버전 조회 실패 userId={}", userId, e);
            return 0L;
        }
    }

    public void appendAndSave(
            Long userId,
            String userMsg,
            String assistantMsg,
            List<ChatAction> actions,
            List<String> suggestions,
            long expectedHistoryVersion
    ) {
        long currentHistoryVersion = getHistoryVersion(userId);
        if (currentHistoryVersion != expectedHistoryVersion) {
            log.info("대화 이력 저장 스킵 - 세션 버전 불일치 userId={} expected={} current={}",
                    userId, expectedHistoryVersion, currentHistoryVersion);
            return;
        }

        List<ChatHistoryMessageResponse> history = getUiHistory(userId);

        history.add(ChatHistoryMessageResponse.builder()
                .role("user")
                .content(userMsg)
                .build());

        history.add(ChatHistoryMessageResponse.builder()
                .role("assistant")
                .content(assistantMsg)
                .actions(actions == null || actions.isEmpty() ? null : actions)
                .suggestions(suggestions == null || suggestions.isEmpty() ? null : suggestions)
                .build());

        while (history.size() > MAX_UI_MESSAGES) {
            history.remove(0);
        }

        save(userId, history);
    }

    public void clearHistory(Long userId) {
        try {
            stringRedisTemplate.opsForValue().increment(VERSION_KEY_PREFIX + userId);
            stringRedisTemplate.delete(KEY_PREFIX + userId);
        } catch (Exception e) {
            log.warn("대화 이력 삭제 실패 userId={}", userId, e);
        }
    }

    private void save(Long userId, List<ChatHistoryMessageResponse> history) {
        String key = KEY_PREFIX + userId;
        try {
            String json = objectMapper.writeValueAsString(history);
            stringRedisTemplate.opsForValue().set(key, json, TTL);
        } catch (Exception e) {
            log.warn("대화 이력 저장 실패 userId={}", userId, e);
        }
    }

}
