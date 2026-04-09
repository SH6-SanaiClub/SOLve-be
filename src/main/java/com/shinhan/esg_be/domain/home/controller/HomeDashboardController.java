package com.shinhan.esg_be.domain.home.controller;

import com.shinhan.esg_be.domain.home.dto.response.HomeDashboardSummaryResponse;
import com.shinhan.esg_be.domain.home.service.HomeDashboardService;
import com.shinhan.esg_be.global.security.AuthContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 홈 대시보드 관련 API를 담당하는 컨트롤러
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/home")
public class HomeDashboardController {

    private final HomeDashboardService homeDashboardService;
    private final AuthContext authContext;

    @GetMapping("/summary")
    public ResponseEntity<HomeDashboardSummaryResponse> getSummary() {
        return ResponseEntity.ok(homeDashboardService.getSummary(authContext.currentUserId()));
    }
}
