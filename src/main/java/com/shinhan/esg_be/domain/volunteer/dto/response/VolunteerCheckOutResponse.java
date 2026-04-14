package com.shinhan.esg_be.domain.volunteer.dto.response;

import com.shinhan.esg_be.domain.volunteer.entity.enums.VolunteerStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class VolunteerCheckOutResponse {

    private final String name;
    private final LocalDateTime checkInAt;
    private final LocalDateTime checkOutAt;
    private final VolunteerStatus status;
    private final int awardedPoint;
    private final int currentPoint;
}
