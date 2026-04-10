package com.shinhan.esg_be.domain.point.controller;

import com.shinhan.esg_be.domain.point.dto.response.MyPointHistoryResponse;
import com.shinhan.esg_be.domain.point.dto.response.MyPointSummaryResponse;
import com.shinhan.esg_be.domain.point.service.MyPointService;
import com.shinhan.esg_be.global.security.AuthContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/my/points")
@RequiredArgsConstructor
public class MyPointController {

    private final MyPointService myPointService;
    private final AuthContext authContext;

    @GetMapping("/summary")
    public ResponseEntity<MyPointSummaryResponse> getSummary() {
        return ResponseEntity.ok(myPointService.getSummary(authContext.currentUserId()));
    }

    @GetMapping("/histories")
    public ResponseEntity<MyPointHistoryResponse> getHistories() {
        return ResponseEntity.ok(myPointService.getHistories(authContext.currentUserId()));
    }
}
