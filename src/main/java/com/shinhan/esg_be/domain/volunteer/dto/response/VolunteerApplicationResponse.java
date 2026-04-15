package com.shinhan.esg_be.domain.volunteer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class VolunteerApplicationResponse {

    private final List<VolunteerApplicationItemResponse> volunteers;
}
