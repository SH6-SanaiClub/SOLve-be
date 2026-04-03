package com.shinhan.esg_be.domain.auth.controller;

import com.shinhan.esg_be.domain.auth.dto.request.AuthJoinRequest;
import com.shinhan.esg_be.domain.auth.dto.request.AuthLoginRequest;
import com.shinhan.esg_be.domain.auth.dto.response.TokenResponse;
import com.shinhan.esg_be.domain.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 회원가입 API
     */
    @PostMapping("/join")
    public ResponseEntity<String> join(@RequestBody @Valid AuthJoinRequest req) {
        authService.join(req);
        return ResponseEntity.status(HttpStatus.CREATED).body("회원가입이 완료되었습니다.");
    }

    /**
     * 로그인 API
     */
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody @Valid AuthLoginRequest req) {
        TokenResponse tokenResponse = authService.login(req);
        return ResponseEntity.ok(tokenResponse);
    }
    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        // 1. 보통 여기서 Redis에 해당 토큰을 '블랙리스트'로 등록하는 로직이 들어갑니다.
        return ResponseEntity.ok("로그아웃 되었습니다. 브라우저의 토큰을 삭제해주세요.");
    }
}