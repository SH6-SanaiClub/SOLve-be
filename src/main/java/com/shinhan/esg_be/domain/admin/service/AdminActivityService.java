package com.shinhan.esg_be.domain.admin.service;

import com.shinhan.esg_be.domain.admin.dto.request.AdminActivityCreateRequest;
import com.shinhan.esg_be.domain.admin.dto.request.AdminActivityUpdateRequest;
import com.shinhan.esg_be.domain.admin.dto.request.AdminDonationCreateRequest;
import com.shinhan.esg_be.domain.admin.dto.request.AdminDonationUpdateRequest;
import com.shinhan.esg_be.domain.admin.dto.request.AdminEcoProductCreateRequest;
import com.shinhan.esg_be.domain.admin.dto.request.AdminEcoProductUpdateRequest;
import com.shinhan.esg_be.domain.admin.dto.response.AdminActivityResponse;
import com.shinhan.esg_be.domain.admin.dto.response.AdminDonationParticipantResponse;
import com.shinhan.esg_be.domain.admin.dto.response.AdminDonationResponse;
import com.shinhan.esg_be.domain.admin.dto.response.AdminEcoProductResponse;
import com.shinhan.esg_be.domain.admin.dto.response.AdminVolunteerParticipantResponse;
import com.shinhan.esg_be.domain.admin.dto.response.AdminVolunteerResponse;
import com.shinhan.esg_be.domain.admin.dto.request.AdminVolunteerCreateRequest;
import com.shinhan.esg_be.domain.admin.dto.request.AdminVolunteerUpdateRequest;
import com.shinhan.esg_be.domain.admin.dto.request.AdminStatusUpdateRequest;
import com.shinhan.esg_be.domain.environment.entity.EnvironmentActivity;
import com.shinhan.esg_be.domain.environment.repository.EnvironmentActivityRepository;
import com.shinhan.esg_be.domain.environment.repository.UserEnvironmentActivityRepository;
import com.shinhan.esg_be.domain.social.entity.Donation;
import com.shinhan.esg_be.domain.social.entity.EcoProduct;
import com.shinhan.esg_be.domain.social.repository.DonationRepository;
import com.shinhan.esg_be.domain.social.repository.EcoProductRepository;
import com.shinhan.esg_be.domain.social.repository.UserDonationRepository;
import com.shinhan.esg_be.domain.social.repository.UserEcoProductRepository;
import com.shinhan.esg_be.domain.volunteer.entity.Volunteer;
import com.shinhan.esg_be.domain.volunteer.repository.UserVolunteerRepository;
import com.shinhan.esg_be.domain.volunteer.repository.VolunteerRepository;
import com.shinhan.esg_be.global.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminActivityService {

    private static final Set<String> SUPPORTED_ACTIVITY_NAMES = Set.of(
            "텀블러 인증",
            "공유자전거 인증",
            "전기차 인증"
    );

    private final EnvironmentActivityRepository environmentActivityRepository;
    private final UserEnvironmentActivityRepository userEnvironmentActivityRepository;
    private final DonationRepository donationRepository;
    private final UserDonationRepository userDonationRepository;
    private final VolunteerRepository volunteerRepository;
    private final UserVolunteerRepository userVolunteerRepository;
    private final EcoProductRepository ecoProductRepository;
    private final UserEcoProductRepository userEcoProductRepository;

    public List<AdminActivityResponse> getActivities() {
        return environmentActivityRepository.findAllByOrderByActivityIdAsc()
                .stream()
                .map(activity -> AdminActivityResponse.from(
                        activity,
                        userEnvironmentActivityRepository.countByActivity_ActivityId(activity.getActivityId())
                ))
                .toList();
    }

    @Transactional
    public AdminActivityResponse createActivity(AdminActivityCreateRequest request) {
        validateSupportedActivityName(request.getName());
        if (environmentActivityRepository.existsByName(request.getName())) {
            throw new BadRequestException("이미 등록된 활동명입니다.");
        }

        EnvironmentActivity savedActivity = environmentActivityRepository.save(
                EnvironmentActivity.create(request.getName())
        );
        return AdminActivityResponse.from(savedActivity, 0L);
    }

    @Transactional
    public AdminActivityResponse updateActivity(Long activityId, AdminActivityUpdateRequest request) {
        validateSupportedActivityName(request.getName());

        EnvironmentActivity activity = environmentActivityRepository.findById(activityId)
                .orElseThrow(() -> new BadRequestException("활동을 찾을 수 없습니다."));

        boolean duplicated = environmentActivityRepository.existsByName(request.getName())
                && !activity.getName().equals(request.getName());
        if (duplicated) {
            throw new BadRequestException("이미 등록된 활동명입니다.");
        }

        activity.updateName(request.getName());
        return AdminActivityResponse.from(
                activity,
                userEnvironmentActivityRepository.countByActivity_ActivityId(activity.getActivityId())
        );
    }

    public AdminActivityResponse updateActivityStatus(Long activityId, AdminStatusUpdateRequest request) {
        if (activityId == null || request.getIsActive() == null) {
            throw new BadRequestException("요청 값이 올바르지 않습니다.");
        }
        throw new BadRequestException("현재 activity 스키마에서는 활성/비활성 변경을 지원하지 않습니다.");
    }

    public List<AdminDonationResponse> getDonations() {
        return donationRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(donation -> AdminDonationResponse.from(
                        donation,
                        userDonationRepository.countByDonation_DonationId(donation.getDonationId()),
                        List.of()
                ))
                .toList();
    }

    public AdminDonationResponse getDonation(Long donationId) {
        Donation donation = getDonationEntity(donationId);
        List<AdminDonationParticipantResponse> participants = userDonationRepository
                .findAllByDonationIdWithUserAndPayment(donationId)
                .stream()
                .map(AdminDonationParticipantResponse::from)
                .toList();

        return AdminDonationResponse.from(donation, participants.size(), participants);
    }

    @Transactional
    public AdminDonationResponse createDonation(AdminDonationCreateRequest request) {
        validateDonationPeriod(request.getStartDate(), request.getEndDate());

        Donation donation = donationRepository.save(
                Donation.create(
                        request.getName(),
                        request.getSummary(),
                        request.getDescription(),
                        request.getTargetAmount(),
                        request.getImageUrl(),
                        request.getStartDate(),
                        request.getEndDate(),
                        request.getIsActive()
                )
        );
        return AdminDonationResponse.from(donation, 0L, List.of());
    }

    @Transactional
    public AdminDonationResponse updateDonation(Long donationId, AdminDonationUpdateRequest request) {
        validateDonationPeriod(request.getStartDate(), request.getEndDate());

        Donation donation = getDonationEntity(donationId);
        donation.update(
                request.getName(),
                request.getSummary(),
                request.getDescription(),
                request.getTargetAmount(),
                request.getImageUrl(),
                request.getStartDate(),
                request.getEndDate()
        );

        return AdminDonationResponse.from(
                donation,
                userDonationRepository.countByDonation_DonationId(donationId),
                List.of()
        );
    }

    @Transactional
    public AdminDonationResponse updateDonationStatus(Long donationId, AdminStatusUpdateRequest request) {
        Donation donation = getDonationEntity(donationId);
        donation.updateStatus(request.getIsActive());
        return AdminDonationResponse.from(
                donation,
                userDonationRepository.countByDonation_DonationId(donationId),
                List.of()
        );
    }

    @Transactional
    public void deleteDonation(Long donationId) {
        Donation donation = getDonationEntity(donationId);
        if (userDonationRepository.countByDonation_DonationId(donationId) > 0) {
            throw new BadRequestException("참여 이력이 있는 기부 캠페인은 삭제할 수 없습니다.");
        }
        donationRepository.delete(donation);
    }

    public List<AdminVolunteerResponse> getVolunteers() {
        return volunteerRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(volunteer -> AdminVolunteerResponse.from(
                        volunteer,
                        userVolunteerRepository.countByVolunteer_VolunteerId(volunteer.getVolunteerId()),
                        List.of()
                ))
                .toList();
    }

    public AdminVolunteerResponse getVolunteer(Long volunteerId) {
        Volunteer volunteer = getVolunteerEntity(volunteerId);
        List<AdminVolunteerParticipantResponse> participants = userVolunteerRepository
                .findAllByVolunteerIdWithUser(volunteerId)
                .stream()
                .map(AdminVolunteerParticipantResponse::from)
                .toList();

        return AdminVolunteerResponse.from(volunteer, participants.size(), participants);
    }

    @Transactional
    public AdminVolunteerResponse createVolunteer(AdminVolunteerCreateRequest request) {
        validateVolunteerQrToken(null, request.getQrToken());

        Volunteer volunteer = volunteerRepository.save(
                Volunteer.create(
                        request.getName(),
                        request.getDescription(),
                        request.getActivityDate(),
                        request.getLocation(),
                        request.getIsActive(),
                        request.getCapacity(),
                        request.getVolunteerHour(),
                        request.getOrganization(),
                        request.getQrToken(),
                        request.getLatitude(),
                        request.getLongitude()
                )
        );
        return AdminVolunteerResponse.from(volunteer, 0L, List.of());
    }

    @Transactional
    public AdminVolunteerResponse updateVolunteer(Long volunteerId, AdminVolunteerUpdateRequest request) {
        Volunteer volunteer = getVolunteerEntity(volunteerId);
        validateVolunteerCapacity(volunteer, request.getCapacity());
        validateVolunteerQrToken(volunteerId, request.getQrToken());

        volunteer.update(
                request.getName(),
                request.getDescription(),
                request.getActivityDate(),
                request.getLocation(),
                request.getCapacity(),
                request.getVolunteerHour(),
                request.getOrganization(),
                request.getQrToken(),
                request.getLatitude(),
                request.getLongitude()
        );

        return AdminVolunteerResponse.from(
                volunteer,
                userVolunteerRepository.countByVolunteer_VolunteerId(volunteerId),
                List.of()
        );
    }

    @Transactional
    public AdminVolunteerResponse updateVolunteerStatus(Long volunteerId, AdminStatusUpdateRequest request) {
        Volunteer volunteer = getVolunteerEntity(volunteerId);
        volunteer.updateStatus(request.getIsActive());
        return AdminVolunteerResponse.from(
                volunteer,
                userVolunteerRepository.countByVolunteer_VolunteerId(volunteerId),
                List.of()
        );
    }

    @Transactional
    public void deleteVolunteer(Long volunteerId) {
        Volunteer volunteer = getVolunteerEntity(volunteerId);
        if (userVolunteerRepository.countByVolunteer_VolunteerId(volunteerId) > 0) {
            throw new BadRequestException("신청 이력이 있는 봉사활동은 삭제할 수 없습니다.");
        }
        volunteerRepository.delete(volunteer);
    }

    public List<AdminEcoProductResponse> getEcoProducts() {
        return ecoProductRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(ecoProduct -> AdminEcoProductResponse.from(
                        ecoProduct,
                        userEcoProductRepository.countByEcoProduct_ProductId(ecoProduct.getProductId())
                ))
                .toList();
    }

    @Transactional
    public AdminEcoProductResponse createEcoProduct(AdminEcoProductCreateRequest request) {
        EcoProduct ecoProduct = ecoProductRepository.save(
                EcoProduct.create(
                        request.getName(),
                        request.getStoreName(),
                        request.getCategory(),
                        request.getPrice(),
                        request.getImageUrl(),
                        request.getDescription(),
                        request.getStock(),
                        request.getIsActive()
                )
        );
        return AdminEcoProductResponse.from(ecoProduct, 0L);
    }

    @Transactional
    public AdminEcoProductResponse updateEcoProduct(Long productId, AdminEcoProductUpdateRequest request) {
        EcoProduct ecoProduct = getEcoProductEntity(productId);
        ecoProduct.update(
                request.getName(),
                request.getStoreName(),
                request.getCategory(),
                request.getPrice(),
                request.getImageUrl(),
                request.getDescription(),
                request.getStock()
        );
        return AdminEcoProductResponse.from(
                ecoProduct,
                userEcoProductRepository.countByEcoProduct_ProductId(productId)
        );
    }

    @Transactional
    public AdminEcoProductResponse updateEcoProductStatus(Long productId, AdminStatusUpdateRequest request) {
        EcoProduct ecoProduct = getEcoProductEntity(productId);
        ecoProduct.updateStatus(request.getIsActive());
        return AdminEcoProductResponse.from(
                ecoProduct,
                userEcoProductRepository.countByEcoProduct_ProductId(productId)
        );
    }

    @Transactional
    public void deleteEcoProduct(Long productId) {
        EcoProduct ecoProduct = getEcoProductEntity(productId);
        if (userEcoProductRepository.countByEcoProduct_ProductId(productId) > 0) {
            throw new BadRequestException("구매 이력이 있는 상품은 삭제할 수 없습니다.");
        }
        ecoProductRepository.delete(ecoProduct);
    }

    private void validateSupportedActivityName(String name) {
        if (!SUPPORTED_ACTIVITY_NAMES.contains(name)) {
            throw new BadRequestException("현재 스키마에서는 텀블러/공유자전거/전기차 인증 활동만 관리할 수 있습니다.");
        }
    }

    private void validateDonationPeriod(java.time.LocalDateTime startDate, java.time.LocalDateTime endDate) {
        if (!startDate.isBefore(endDate)) {
            throw new BadRequestException("기부 시작일은 종료일보다 빨라야 합니다.");
        }
    }

    private void validateVolunteerCapacity(Volunteer volunteer, Integer capacity) {
        if (capacity < volunteer.getCurrentEnrolled()) {
            throw new BadRequestException("현재 신청 인원보다 작은 정원으로 수정할 수 없습니다.");
        }
    }

    private void validateVolunteerQrToken(Long volunteerId, String qrToken) {
        boolean exists = volunteerId == null
                ? volunteerRepository.existsByQrToken(qrToken)
                : volunteerRepository.existsByQrTokenAndVolunteerIdNot(qrToken, volunteerId);
        if (exists) {
            throw new BadRequestException("이미 사용 중인 QR 토큰입니다.");
        }
    }

    private Donation getDonationEntity(Long donationId) {
        return donationRepository.findById(donationId)
                .orElseThrow(() -> new BadRequestException("기부 캠페인을 찾을 수 없습니다."));
    }

    private Volunteer getVolunteerEntity(Long volunteerId) {
        return volunteerRepository.findById(volunteerId)
                .orElseThrow(() -> new BadRequestException("봉사활동을 찾을 수 없습니다."));
    }

    private EcoProduct getEcoProductEntity(Long productId) {
        return ecoProductRepository.findById(productId)
                .orElseThrow(() -> new BadRequestException("상품을 찾을 수 없습니다."));
    }
}
