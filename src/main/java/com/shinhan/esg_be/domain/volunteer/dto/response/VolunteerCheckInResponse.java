package com.shinhan.esg_be.domain.volunteer.dto.response;

import com.shinhan.esg_be.domain.volunteer.entity.enums.VolunteerStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class VolunteerCheckInResponse {

    private final LocalDateTime checkInAt;
    private final VolunteerStatus status;
}
