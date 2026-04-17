package com.shinhan.esg_be.domain.volunteer.dto.response;

import com.shinhan.esg_be.domain.volunteer.entity.enums.VolunteerStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class VolunteerDetailResponse {

    private final Long volunteerId;
    private final String name;
    private final String description;
    private final String imageUrl;
    private final LocalDateTime activityDate;
    private final String location;
    private final Integer capacity;
    private final Integer currentEnrolled;
    private final Integer volunteerHour;
    private final String organization;
    private final VolunteerStatus status;
}
