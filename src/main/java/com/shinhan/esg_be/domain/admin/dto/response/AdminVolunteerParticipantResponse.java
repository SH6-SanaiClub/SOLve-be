package com.shinhan.esg_be.domain.admin.dto.response;

import com.shinhan.esg_be.domain.volunteer.entity.UserVolunteer;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AdminVolunteerParticipantResponse {

    private Long userId;
    private String loginId;
    private String name;
    private String status;
    private Integer volunteerHour;
    private LocalDateTime checkInAt;
    private LocalDateTime checkOutAt;
    private LocalDateTime createdAt;

    public static AdminVolunteerParticipantResponse from(UserVolunteer userVolunteer) {
        return new AdminVolunteerParticipantResponse(
                userVolunteer.getUser().getUserId(),
                userVolunteer.getUser().getLoginId(),
                userVolunteer.getUser().getName(),
                userVolunteer.getStatus().name(),
                userVolunteer.getVolunteerHour(),
                userVolunteer.getCheckInAt(),
                userVolunteer.getCheckOutAt(),
                userVolunteer.getCreatedAt()
        );
    }
}
