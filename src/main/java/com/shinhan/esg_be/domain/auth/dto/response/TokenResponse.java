package com.shinhan.esg_be.domain.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor // 생성자를 자동으로 만들어줌
public class TokenResponse {
    private String accessToken;
    private final String tokenType = "Bearer"; // JWT 표준 타입 명시
}