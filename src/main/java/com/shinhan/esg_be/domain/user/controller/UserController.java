package com.shinhan.esg_be.domain.user.controller;

import com.shinhan.esg_be.domain.user.dto.response.UserMeResponse;
import com.shinhan.esg_be.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserMeResponse> getMyInfo(@AuthenticationPrincipal String loginId) {
        return ResponseEntity.ok(userService.getMyInfo(loginId));
    }
}
