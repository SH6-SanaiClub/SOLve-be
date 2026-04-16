package com.shinhan.esg_be.domain.admin.controller;

import com.shinhan.esg_be.domain.admin.dto.request.AdminPenaltyRequest;
import com.shinhan.esg_be.domain.admin.dto.request.AdminUserStatusUpdateRequest;
import com.shinhan.esg_be.domain.admin.dto.response.AdminUserDetailResponse;
import com.shinhan.esg_be.domain.admin.dto.response.AdminUserListResponse;
import com.shinhan.esg_be.domain.admin.dto.response.AdminPageResponse;
import com.shinhan.esg_be.domain.admin.service.AdminUserService;
import org.springdoc.core.annotations.ParameterObject;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public ResponseEntity<AdminPageResponse<AdminUserListResponse>> getUsers(
            @RequestParam(required = false) String keyword,
            @ParameterObject
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return ResponseEntity.ok(adminUserService.getUsers(keyword, pageable));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<AdminUserDetailResponse> getUser(@PathVariable Long userId) {
        return ResponseEntity.ok(adminUserService.getUser(userId));
    }

    @PatchMapping("/{userId}/status")
    public ResponseEntity<AdminUserDetailResponse> updateUserStatus(
            @PathVariable Long userId,
            @RequestBody @Valid AdminUserStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(adminUserService.updateUserStatus(userId, request));
    }

    @PostMapping("/{userId}/penalty")
    public ResponseEntity<AdminUserDetailResponse> applyPenalty(
            @PathVariable Long userId,
            @RequestBody @Valid AdminPenaltyRequest request
    ) {
        return ResponseEntity.ok(adminUserService.applyPenalty(userId, request));
    }
}
