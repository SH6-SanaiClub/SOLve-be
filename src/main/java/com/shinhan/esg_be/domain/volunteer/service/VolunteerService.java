package com.shinhan.esg_be.domain.volunteer.service;

import com.shinhan.esg_be.domain.point.service.result.ApplyActivityPointResult;
import com.shinhan.esg_be.domain.reward.service.RewardService;
import com.shinhan.esg_be.domain.reward.service.command.ApplyActivityRewardCommand;
import com.shinhan.esg_be.domain.reward.service.result.ApplyActivityRewardResult;
import com.shinhan.esg_be.domain.user.entity.User;
import com.shinhan.esg_be.domain.user.repository.UserRepository;
import com.shinhan.esg_be.domain.volunteer.dto.request.VolunteerApplyRequest;
import com.shinhan.esg_be.domain.volunteer.dto.request.VolunteerCheckInRequest;
import com.shinhan.esg_be.domain.volunteer.dto.request.VolunteerCheckOutRequest;
import com.shinhan.esg_be.domain.volunteer.dto.response.VolunteerApplyResponse;
import com.shinhan.esg_be.domain.volunteer.dto.response.VolunteerApplicationResponse;
import com.shinhan.esg_be.domain.volunteer.dto.response.VolunteerAttendanceResponse;
import com.shinhan.esg_be.domain.volunteer.dto.response.VolunteerCheckInResponse;
import com.shinhan.esg_be.domain.volunteer.dto.response.VolunteerCheckOutResponse;
import com.shinhan.esg_be.domain.volunteer.dto.response.VolunteerItemResponse;
import com.shinhan.esg_be.domain.volunteer.dto.response.VolunteerDetailResponse;
import com.shinhan.esg_be.domain.volunteer.dto.response.VolunteerResponse;
import com.shinhan.esg_be.domain.volunteer.entity.UserVolunteer;
import com.shinhan.esg_be.domain.volunteer.entity.Volunteer;
import com.shinhan.esg_be.domain.volunteer.entity.enums.VolunteerStatus;
import com.shinhan.esg_be.domain.volunteer.repository.VolunteerRepository;
import com.shinhan.esg_be.domain.volunteer.repository.UserVolunteerRepository;
import com.shinhan.esg_be.global.common.enums.ActivityType;
import com.shinhan.esg_be.global.exception.BadRequestException;
import com.shinhan.esg_be.global.security.AuthContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
@Transactional
public class VolunteerService {

    private final AuthContext authContext;
    private final VolunteerRepository volunteerRepository;
    private final UserVolunteerRepository userVolunteerRepository;
    private final UserRepository userRepository;
    private final RewardService rewardService;
    private final VolunteerStatusTransitionService volunteerStatusTransitionService;

    private static final double CHECK_IN_RADIUS_METERS = 100.0;

    public VolunteerResponse getVolunteers() {
        return new VolunteerResponse(
                volunteerRepository.findActiveVolunteerList(LocalDateTime.now())
        );
    }

    @Transactional(readOnly = true)
    public VolunteerApplicationResponse getMyVolunteerApplications() {
        Long userId = authContext.currentUserId();
        return new VolunteerApplicationResponse(
                userVolunteerRepository.findApplicationVolunteersByUserId(userId, LocalDateTime.now())
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
                volunteer.getImageUrl(),
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
        volunteerStatusTransitionService.scheduleTransition(userVolunteer, getCheckOutDeadline(volunteer));

        return new VolunteerApplyResponse(
                userVolunteer.getVolunteerApplicationsId(),
                volunteer.getVolunteerId(),
                volunteer.getName(),
                volunteer.getLocation(),
                volunteer.getActivityDate(),
                userVolunteer.getStatus()
        );
    }

    public void cancelVolunteerApplication(Long volunteerApplicationId) {
        Long userId = authContext.currentUserId();
        LocalDateTime now = LocalDateTime.now();

        UserVolunteer userVolunteer = userVolunteerRepository
                .findByVolunteerApplicationsIdAndUser_UserIdAndStatus(
                        volunteerApplicationId,
                        userId,
                        VolunteerStatus.APPLIED
                )
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "신청한 봉사활동을 찾을 수 없습니다."));

        Volunteer volunteer = userVolunteer.getVolunteer();
        LocalDateTime cancelDeadline = volunteer.getActivityDate().minusHours(24);
        if (now.isAfter(cancelDeadline)) {
            throw new BadRequestException("봉사 시작 24시간 전까지만 신청 취소가 가능합니다.");
        }

        int updatedRows = volunteerRepository.decreaseCurrentEnrolled(volunteer.getVolunteerId());
        if (updatedRows == 0) {
            throw new BadRequestException("봉사 신청 취소에 실패했습니다.");
        }

        userVolunteerRepository.delete(userVolunteer);
        volunteerStatusTransitionService.clearTransition(userVolunteer.getVolunteerApplicationsId());
    }

    @Transactional(readOnly = true)
    public VolunteerAttendanceResponse getVolunteerAttendance(String qrToken) {
        Long userId = authContext.currentUserId();

        Volunteer volunteer = volunteerRepository.findByQrTokenAndIsActiveTrue(qrToken)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "유효한 QR 정보가 없습니다."));

        UserVolunteer userVolunteer = userVolunteerRepository
                .findByUser_UserIdAndVolunteer_VolunteerId(userId, volunteer.getVolunteerId())
                .orElseThrow(() -> new BadRequestException("신청한 봉사활동만 접근할 수 있습니다."));

        return new VolunteerAttendanceResponse(
                userVolunteer.getUser().getName(),
                volunteer.getVolunteerId(),
                volunteer.getName(),
                volunteer.getLocation(),
                volunteer.getActivityDate(),
                volunteer.getVolunteerHour(),
                volunteer.getOrganization(),
                userVolunteer.getStatus(),
                userVolunteer.getCheckInAt(),
                userVolunteer.getCheckOutAt()
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

        userVolunteer.markCheckIn(now, isLateCheckIn(volunteer, now));

        return new VolunteerCheckInResponse(
                userVolunteer.getCheckInAt(),
                userVolunteer.getStatus()
        );
    }

    public VolunteerCheckOutResponse checkOutVolunteer(VolunteerCheckOutRequest request) {
        Long userId = authContext.currentUserId();
        LocalDateTime now = LocalDateTime.now();

        Volunteer volunteer = volunteerRepository.findByQrTokenAndIsActiveTrue(request.getQrToken())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "유효한 QR 정보가 없습니다."));

        UserVolunteer userVolunteer = userVolunteerRepository
                .findByUser_UserIdAndVolunteer_VolunteerId(userId, volunteer.getVolunteerId())
                .orElseThrow(() -> new BadRequestException("출석 완료된 봉사활동만 퇴실할 수 있습니다."));

        if (userVolunteer.getCheckInAt() == null ||
                (userVolunteer.getStatus() != VolunteerStatus.ATTENDED
                        && userVolunteer.getStatus() != VolunteerStatus.INCOMPLETE)) {
            throw new BadRequestException("출석 완료된 봉사활동만 퇴실할 수 있습니다.");
        }

        if (userVolunteer.getCheckOutAt() != null) {
            throw new BadRequestException("이미 퇴실 처리된 봉사활동입니다.");
        }

        validateLocation(volunteer, request.getLatitude(), request.getLongitude());

        boolean completed = isCompletedCheckOut(volunteer, now)
                && userVolunteer.getStatus() == VolunteerStatus.ATTENDED;
        int recognizedVolunteerHour = calculateRecognizedVolunteerHour(volunteer, userVolunteer, now);
        userVolunteer.markCheckOut(now, completed, recognizedVolunteerHour);
        volunteerStatusTransitionService.clearTransition(userVolunteer.getVolunteerApplicationsId());

        int awardedPoint = 0;
        int currentPoint = userVolunteer.getUser().getTotalPoints();

        if (completed) {
            ApplyActivityRewardResult rewardResult = rewardService.applyActivityReward(
                    new ApplyActivityRewardCommand(
                            userId,
                            ActivityType.VOLUNTEER,
                            null,
                            now
                    )
            );
            ApplyActivityPointResult pointResult = rewardResult.pointResult();
            awardedPoint = pointResult.activityPoint() + pointResult.bonusPoint();
            currentPoint = pointResult.pointAfter();
        }

        return new VolunteerCheckOutResponse(
                volunteer.getName(),
                userVolunteer.getCheckInAt(),
                userVolunteer.getCheckOutAt(),
                userVolunteer.getStatus(),
                awardedPoint,
                currentPoint
        );
    }

    private void validateCheckInTime(Volunteer volunteer, LocalDateTime now) {
        LocalDateTime availableAt = volunteer.getActivityDate().minusHours(1);
        LocalDateTime deadline = getCheckOutDeadline(volunteer);

        if (now.isBefore(availableAt) || now.isAfter(deadline)) {
            throw new BadRequestException("출석 가능한 시간이 아닙니다.");
        }
    }

    private boolean isLateCheckIn(Volunteer volunteer, LocalDateTime now) {
        return now.isAfter(getCheckInDeadline(volunteer));
    }

    private boolean isCompletedCheckOut(Volunteer volunteer, LocalDateTime now) {
        LocalDateTime availableAt = volunteer.getActivityDate()
                .plusHours(volunteer.getVolunteerHour())
                .minusMinutes(1);
        LocalDateTime deadline = getCheckOutDeadline(volunteer);

        return !now.isBefore(availableAt) && !now.isAfter(deadline);
    }

    private LocalDateTime getCheckInDeadline(Volunteer volunteer) {
        return volunteer.getActivityDate()
                .plusMinutes((long) volunteer.getVolunteerHour() * 5L);
    }

    private LocalDateTime getCheckOutDeadline(Volunteer volunteer) {
        return volunteer.getActivityDate()
                .plusHours(volunteer.getVolunteerHour())
                .plusHours(1);
    }

    private int calculateRecognizedVolunteerHour(
            Volunteer volunteer,
            UserVolunteer userVolunteer,
            LocalDateTime checkedOutAt
    ) {
        if (userVolunteer.getCheckInAt() == null) {
            return 0;
        }

        LocalDateTime scheduledStart = volunteer.getActivityDate();
        LocalDateTime scheduledEnd = scheduledStart.plusHours(volunteer.getVolunteerHour());

        LocalDateTime recognizedStart = userVolunteer.getCheckInAt().isAfter(getCheckInDeadline(volunteer))
                ? ceilToVolunteerHourBoundary(scheduledStart, userVolunteer.getCheckInAt())
                : scheduledStart;

        LocalDateTime recognizedEnd = checkedOutAt.isBefore(scheduledEnd.minusMinutes(1))
                ? floorToVolunteerHourBoundary(scheduledStart, checkedOutAt)
                : scheduledEnd;

        if (!recognizedEnd.isAfter(recognizedStart)) {
            return 0;
        }

        long recognizedHours = Duration.between(recognizedStart, recognizedEnd).toHours();
        return (int) Math.max(0, Math.min(volunteer.getVolunteerHour(), recognizedHours));
    }

    private LocalDateTime ceilToVolunteerHourBoundary(LocalDateTime scheduledStart, LocalDateTime dateTime) {
        if (!dateTime.isAfter(scheduledStart)) {
            return scheduledStart;
        }

        long minutes = Duration.between(scheduledStart, dateTime).toMinutes();
        long hoursToAdd = (minutes + 59) / 60;
        return scheduledStart.plusHours(hoursToAdd);
    }

    private LocalDateTime floorToVolunteerHourBoundary(LocalDateTime scheduledStart, LocalDateTime dateTime) {
        if (!dateTime.isAfter(scheduledStart)) {
            return scheduledStart;
        }

        long minutes = Duration.between(scheduledStart, dateTime).toMinutes();
        long hoursToAdd = minutes / 60;
        return scheduledStart.plusHours(hoursToAdd);
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
