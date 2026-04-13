package com.shinhan.esg_be.domain.volunteer.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class VolunteerApplyRequest {

    @NotNull
    private Long volunteerId;
}
