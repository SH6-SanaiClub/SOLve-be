package com.shinhan.esg_be.domain.volunteer.service;

import com.shinhan.esg_be.domain.user.entity.User;
import com.shinhan.esg_be.domain.user.repository.UserRepository;
import com.shinhan.esg_be.domain.volunteer.dto.request.VolunteerApplyRequest;
import com.shinhan.esg_be.domain.volunteer.dto.response.VolunteerApplyResponse;
import com.shinhan.esg_be.domain.volunteer.dto.response.VolunteerItemResponse;
import com.shinhan.esg_be.domain.volunteer.dto.response.VolunteerDetailResponse;
import com.shinhan.esg_be.domain.volunteer.dto.response.VolunteerResponse;
import com.shinhan.esg_be.domain.volunteer.entity.UserVolunteer;
import com.shinhan.esg_be.domain.volunteer.entity.Volunteer;
import com.shinhan.esg_be.domain.volunteer.entity.enums.VolunteerStatus;
import com.shinhan.esg_be.domain.volunteer.repository.VolunteerRepository;
import com.shinhan.esg_be.domain.volunteer.repository.UserVolunteerRepository;
import com.shinhan.esg_be.global.exception.BadRequestException;
import com.shinhan.esg_be.global.security.AuthContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
@Transactional
public class VolunteerService {

    private final AuthContext authContext;
    private final VolunteerRepository volunteerRepository;
    private final UserVolunteerRepository userVolunteerRepository;
    private final UserRepository userRepository;

    public VolunteerResponse getVolunteers() {
        return new VolunteerResponse(
                volunteerRepository.findActiveVolunteerList(LocalDateTime.now())
        );
    }

    public VolunteerDetailResponse getVolunteer(Long volunteerId) {
        Long userId = authContext.currentUserId();
        Volunteer volunteer = volunteerRepository.findActiveVolunteerDetail(volunteerId, LocalDateTime.now())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "봉사활동을 찾을 수 없습니다."));

        VolunteerStatus status = null;
        if (userVolunteerRepository.existsByUser_UserIdAndVolunteer_VolunteerIdAndStatusNot(
                userId,
                volunteer.getVolunteerId(),
                VolunteerStatus.NOSHOW
        )) {
            status = VolunteerStatus.APPLIED;
        }

        return new VolunteerDetailResponse(
                volunteer.getVolunteerId(),
                volunteer.getName(),
                volunteer.getDescription(),
                volunteer.getActivityDate(),
                volunteer.getLocation(),
                volunteer.getCapacity(),
                volunteer.getCurrentEnrolled(),
                volunteer.getVolunteerHour(),
                volunteer.getOrganization(),
                status
        );
    }

    public VolunteerApplyResponse applyVolunteer(VolunteerApplyRequest request) {
        Long userId = authContext.currentUserId();
        LocalDateTime now = LocalDateTime.now();

        Volunteer volunteer = volunteerRepository.findApplicableVolunteer(request.getVolunteerId(), now)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "봉사활동을 찾을 수 없습니다."));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "사용자를 찾을 수 없습니다."));

        if (userVolunteerRepository.existsByUser_UserIdAndVolunteer_VolunteerIdAndStatusNot(
                userId,
                volunteer.getVolunteerId(),
                VolunteerStatus.NOSHOW
        )) {
            throw new BadRequestException("이미 신청한 봉사활동입니다.");
        }

        int updatedRows = volunteerRepository.increaseCurrentEnrolled(volunteer.getVolunteerId());
        if (updatedRows == 0) {
            throw new BadRequestException("모집이 마감된 봉사활동입니다.");
        }

        UserVolunteer userVolunteer = userVolunteerRepository.save(UserVolunteer.create(user, volunteer));

        return new VolunteerApplyResponse(
                userVolunteer.getVolunteerApplicationsId(),
                volunteer.getVolunteerId(),
                volunteer.getName(),
                volunteer.getLocation(),
                volunteer.getActivityDate(),
                userVolunteer.getStatus()
        );
    }
}
