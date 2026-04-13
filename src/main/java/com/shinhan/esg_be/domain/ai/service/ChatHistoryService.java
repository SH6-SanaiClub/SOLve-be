package com.shinhan.esg_be.domain.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatHistoryService {

    private static final String KEY_PREFIX = "chat:history:";
    private static final Duration TTL = Duration.ofMinutes(30);
    private static final int MAX_MESSAGES = 10;

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public List<Map<String, String>> getHistory(Long userId) {
        String key = KEY_PREFIX + userId;
        try {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (json == null) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(json, new TypeReference<List<Map<String, String>>>() {
            });
        } catch (Exception e) {
            log.warn("대화 이력 조회 실패 userId={}", userId, e);
            return new ArrayList<>();
        }
    }

    public void appendAndSave(Long userId, String userMsg, String assistantMsg) {
        List<Map<String, String>> history = getHistory(userId);

        Map<String, String> userTurn = new HashMap<>();
        userTurn.put("role", "user");
        userTurn.put("content", userMsg);

        Map<String, String> assistantTurn = new HashMap<>();
        assistantTurn.put("role", "assistant");
        assistantTurn.put("content", assistantMsg);

        history.add(userTurn);
        history.add(assistantTurn);

        while (history.size() > MAX_MESSAGES) {
            history.remove(0);
        }

        save(userId, history);
    }

    public void clearHistory(Long userId) {
        try {
            stringRedisTemplate.delete(KEY_PREFIX + userId);
        } catch (Exception e) {
            log.warn("대화 이력 삭제 실패 userId={}", userId, e);
        }
    }

    private void save(Long userId, List<Map<String, String>> history) {
        String key = KEY_PREFIX + userId;
        try {
            String json = objectMapper.writeValueAsString(history);
            stringRedisTemplate.opsForValue().set(key, json, TTL);
        } catch (Exception e) {
            log.warn("대화 이력 저장 실패 userId={}", userId, e);
        }
    }
}
