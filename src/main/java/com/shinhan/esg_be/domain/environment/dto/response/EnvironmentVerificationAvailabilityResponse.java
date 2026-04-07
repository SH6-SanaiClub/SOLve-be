package com.shinhan.esg_be.domain.environment.dto.response;

import com.shinhan.esg_be.domain.environment.entity.enums.EnvironmentActivityType;
import lombok.Getter;

@Getter
public class EnvironmentVerificationAvailabilityResponse {

    private final EnvironmentActivityType activityType;
    private final Boolean attemptedToday;

    public EnvironmentVerificationAvailabilityResponse(
            EnvironmentActivityType activityType,
            Boolean attemptedToday
    ) {
        this.activityType = activityType;
        this.attemptedToday = attemptedToday;
    }
}
