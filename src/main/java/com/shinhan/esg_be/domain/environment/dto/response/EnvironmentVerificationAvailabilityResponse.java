package com.shinhan.esg_be.domain.environment.dto.response;

import com.shinhan.esg_be.domain.environment.entity.enums.EnvironmentActivityType;
import lombok.Getter;

@Getter
public class EnvironmentVerificationAvailabilityResponse {

    private final EnvironmentActivityType activityType;
    private final Boolean attemptedToday;
    private final Boolean approved;

    public EnvironmentVerificationAvailabilityResponse(
            EnvironmentActivityType activityType,
            Boolean attemptedToday,
            Boolean approved
    ) {
        this.activityType = activityType;
        this.attemptedToday = attemptedToday;
        this.approved = approved;
    }
}
