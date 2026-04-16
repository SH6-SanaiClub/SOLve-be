package com.shinhan.esg_be.domain.survey.dto;

import com.shinhan.esg_be.domain.user.entity.enums.UserType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;

@Getter
public class SurveySubmitRequest {

    private UserType userType;

    @Min(0)
    @Max(2)
    private Integer environmentWeight;

    @Min(0)
    @Max(2)
    private Integer socialWeight;

    @Min(0)
    @Max(2)
    private Integer financeWeight;

    public boolean hasAllWeights() {
        return environmentWeight != null && socialWeight != null && financeWeight != null;
    }

    public boolean hasAnyWeights() {
        return environmentWeight != null || socialWeight != null || financeWeight != null;
    }
}
