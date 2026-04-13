package com.shinhan.esg_be.domain.survey.dto;

import com.shinhan.esg_be.domain.user.entity.enums.UserType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class SurveySubmitRequest {

    @NotNull
    private UserType userType;
}
