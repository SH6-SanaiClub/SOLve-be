package com.shinhan.esg_be.domain.ai.controller;

import com.shinhan.esg_be.domain.ai.dto.ChatMessageRequest;
import com.shinhan.esg_be.domain.ai.service.ChatService;
import com.shinhan.esg_be.global.security.AuthContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final AuthContext authContext;

    @PostMapping(value = "/messages", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody @Valid ChatMessageRequest req) {
        return chatService.streamChat(authContext.currentUserId(), req.getMessage());
    }
}
