package com.shinhan.esg_be.domain.volunteer.dto.response;

import com.shinhan.esg_be.domain.volunteer.entity.enums.VolunteerStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class VolunteerAttendanceResponse {

    private final String userName;
    private final Long volunteerId;
    private final String name;
    private final String location;
    private final LocalDateTime activityDate;
    private final Integer volunteerHour;
    private final String organization;
    private final VolunteerStatus status;
    private final LocalDateTime checkInAt;
    private final LocalDateTime checkOutAt;
}
