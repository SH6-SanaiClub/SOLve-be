package com.shinhan.esg_be.domain.volunteer.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class VolunteerCheckOutRequest {

    @NotBlank
    private String qrToken;

    @NotNull
    private Double latitude;

    @NotNull
    private Double longitude;
}
