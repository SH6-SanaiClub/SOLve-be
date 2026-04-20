package com.shinhan.esg_be.domain.volunteer.dto.response;

import com.shinhan.esg_be.domain.volunteer.entity.enums.VolunteerStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class VolunteerHistoryItemResponse {

    private final Long volunteerApplicationId;
    private final Long volunteerId;
    private final String organization;
    private final String name;
    private final LocalDateTime activityDate;
    private final VolunteerStatus status;
    private final LocalDateTime checkInAt;
    private final LocalDateTime checkOutAt;
    private final Integer recognizedVolunteerHour;
    private final Integer scheduledVolunteerHour;
}
