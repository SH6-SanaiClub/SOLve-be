package com.shinhan.esg_be.domain.survey.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SurveyStatusResponse {

    private boolean surveyCompleted;
    private String userType;
}
