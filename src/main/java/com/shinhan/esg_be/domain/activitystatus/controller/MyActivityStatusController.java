package com.shinhan.esg_be.domain.activitystatus.controller;

import com.shinhan.esg_be.domain.activitystatus.dto.response.ActivityStatusLogResponse;
import com.shinhan.esg_be.domain.activitystatus.dto.response.ActivityStatusOverviewResponse;
import com.shinhan.esg_be.domain.activitystatus.service.MyActivityStatusService;
import com.shinhan.esg_be.global.security.AuthContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/my/activity-status")
public class MyActivityStatusController {

    private final MyActivityStatusService myActivityStatusService;
    private final AuthContext authContext;

    @GetMapping("/overview")
    public ResponseEntity<ActivityStatusOverviewResponse> getOverview() {
        return ResponseEntity.ok(
                myActivityStatusService.getOverview(authContext.currentUserId())
        );
    }

    @GetMapping("/logs")
    public ResponseEntity<ActivityStatusLogResponse> getLogs(
            @RequestParam(defaultValue = "ALL") String category,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String cursor
    ) {
        return ResponseEntity.ok(
                myActivityStatusService.getLogs(
                        authContext.currentUserId(),
                        category,
                        size,
                        cursor
                )
        );
    }
}
