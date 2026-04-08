package com.shinhan.esg_be.domain.user.controller;

import com.shinhan.esg_be.domain.user.dto.response.UserMeResponse;
import com.shinhan.esg_be.domain.user.service.UserService;
import com.shinhan.esg_be.global.security.AuthContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthContext authContext;

    @GetMapping("/me")
    public ResponseEntity<UserMeResponse> getMyInfo() {
        return ResponseEntity.ok(userService.getMyInfo(authContext.currentLoginId()));
    }
}
