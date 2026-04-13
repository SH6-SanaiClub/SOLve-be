package com.shinhan.esg_be.domain.volunteer.service;

import com.shinhan.esg_be.domain.user.entity.User;
import com.shinhan.esg_be.domain.user.repository.UserRepository;
import com.shinhan.esg_be.domain.volunteer.dto.request.VolunteerApplyRequest;
import com.shinhan.esg_be.domain.volunteer.dto.request.VolunteerCheckInRequest;
import com.shinhan.esg_be.domain.volunteer.dto.response.VolunteerApplyResponse;
import com.shinhan.esg_be.domain.volunteer.dto.response.VolunteerCheckInResponse;
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

    private static final double CHECK_IN_RADIUS_METERS = 100.0;

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
        if (userVolunteerRepository.existsByUser_UserIdAndVolunteer_VolunteerIdAndStatus(
                userId,
                volunteer.getVolunteerId(),
                VolunteerStatus.APPLIED
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

        if (userVolunteerRepository.existsByUser_UserIdAndVolunteer_VolunteerIdAndStatus(
                userId,
                volunteer.getVolunteerId(),
                VolunteerStatus.APPLIED
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

    public VolunteerCheckInResponse checkInVolunteer(VolunteerCheckInRequest request) {
        Long userId = authContext.currentUserId();
        LocalDateTime now = LocalDateTime.now();

        Volunteer volunteer = volunteerRepository.findByQrTokenAndIsActiveTrue(request.getQrToken())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "유효한 QR 정보가 없습니다."));

        UserVolunteer userVolunteer = userVolunteerRepository
                .findByUser_UserIdAndVolunteer_VolunteerIdAndStatus(
                        userId,
                        volunteer.getVolunteerId(),
                        VolunteerStatus.APPLIED
                )
                .orElseThrow(() -> new BadRequestException("신청한 봉사활동만 출석할 수 있습니다."));

        if (userVolunteer.getCheckInAt() != null) {
            throw new BadRequestException("이미 출석 처리된 봉사활동입니다.");
        }

        validateCheckInTime(volunteer, now);
        validateLocation(volunteer, request.getLatitude(), request.getLongitude());

        userVolunteer.markCheckIn(now);

        return new VolunteerCheckInResponse(
                userVolunteer.getCheckInAt(),
                userVolunteer.getStatus()
        );
    }

    private void validateCheckInTime(Volunteer volunteer, LocalDateTime now) {
        LocalDateTime availableAt = volunteer.getActivityDate().minusHours(1);
        LocalDateTime deadline = volunteer.getActivityDate()
                .plusMinutes((long) volunteer.getVolunteerHour() * 5L);

        if (now.isBefore(availableAt) || now.isAfter(deadline)) {
            throw new BadRequestException("출석 가능한 시간이 아닙니다.");
        }
    }

    private void validateLocation(Volunteer volunteer, Double latitude, Double longitude) {
        if (volunteer.getLatitude() == null || volunteer.getLongitude() == null) {
            throw new BadRequestException("봉사활동 위치 정보가 설정되지 않았습니다.");
        }

        double distance = calculateDistanceMeters(
                volunteer.getLatitude(),
                volunteer.getLongitude(),
                latitude,
                longitude
        );

        if (distance > CHECK_IN_RADIUS_METERS) {
            throw new BadRequestException("봉사활동 장소 반경 100m 이내에서만 출석할 수 있습니다.");
        }
    }

    private double calculateDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
        final double earthRadius = 6_371_000;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return earthRadius * c;
    }
}
