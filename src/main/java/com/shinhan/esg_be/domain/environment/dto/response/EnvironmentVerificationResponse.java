package com.shinhan.esg_be.domain.environment.dto.response;

import com.shinhan.esg_be.domain.environment.entity.enums.EnvironmentActivityType;
import lombok.Getter;

@Getter
public class EnvironmentVerificationResponse {

    private final Long verificationId;
    private final EnvironmentActivityType activityType;
    private final Boolean approved;
    private final Integer rewardPoint;

    public EnvironmentVerificationResponse(
            Long verificationId,
            EnvironmentActivityType activityType,
            Boolean approved,
            Integer rewardPoint
    ) {
        this.verificationId = verificationId;
        this.activityType = activityType;
        this.approved = approved;
        this.rewardPoint = rewardPoint;
    }
}
