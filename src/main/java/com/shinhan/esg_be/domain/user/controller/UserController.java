package com.shinhan.esg_be.domain.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getMyInfo(@AuthenticationPrincipal String loginId) {
        // Map은 add가 아니라 put을 사용합니다!
        Map<String, Object> response = new HashMap<>();

        response.put("loginId", loginId);
        response.put("message", "인증된 사용자의 정보를 성공적으로 가져왔습니다.");

        return ResponseEntity.ok(response);
    }
}