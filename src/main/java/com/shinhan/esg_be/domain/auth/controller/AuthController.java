package com.shinhan.esg_be.domain.auth.controller;

import com.shinhan.esg_be.domain.auth.dto.request.AuthJoinRequest;
import com.shinhan.esg_be.domain.auth.dto.request.AuthLoginRequest;
import com.shinhan.esg_be.domain.auth.dto.request.RefreshTokenRequest;
import com.shinhan.esg_be.domain.auth.dto.response.TokenResponse;
import com.shinhan.esg_be.domain.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/join")
    public ResponseEntity<String> join(@RequestBody @Valid AuthJoinRequest req) {
        authService.join(req);
        return ResponseEntity.status(HttpStatus.CREATED).body("회원가입이 완료되었습니다.");
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody @Valid AuthLoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @PostMapping("/reissue")
    public ResponseEntity<TokenResponse> reissue(@RequestBody @Valid RefreshTokenRequest req) {
        return ResponseEntity.ok(authService.reissue(req.getRefreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestBody @Valid RefreshTokenRequest req) {
        authService.logout(req.getRefreshToken());
        return ResponseEntity.ok("로그아웃이 완료되었습니다.");
    }

    @GetMapping("/check-id")
    public ResponseEntity<Boolean> checkId(@RequestParam String loginId) {
        return ResponseEntity.ok(authService.checkLoginIdDuplicate(loginId));
    }
}
