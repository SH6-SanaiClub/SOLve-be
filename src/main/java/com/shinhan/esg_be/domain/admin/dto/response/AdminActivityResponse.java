package com.shinhan.esg_be.domain.admin.dto.response;

import com.shinhan.esg_be.domain.environment.entity.EnvironmentActivity;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AdminActivityResponse {

    private Long activityId;
    private String name;
    private Long verificationCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AdminActivityResponse from(EnvironmentActivity activity, long verificationCount) {
        return new AdminActivityResponse(
                activity.getActivityId(),
                activity.getName(),
                verificationCount,
                activity.getCreatedAt(),
                activity.getUpdatedAt()
        );
    }
}
