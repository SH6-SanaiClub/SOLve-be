package com.shinhan.esg_be.domain.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatHistoryMessageResponse {
    private String role;
    private String content;
    private List<ChatAction> actions;
    private List<String> suggestions;
}
