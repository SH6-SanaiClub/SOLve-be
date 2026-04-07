package com.shinhan.esg_be.domain.environment.service;

import com.shinhan.esg_be.domain.environment.dto.request.EnvironmentVerificationCreateRequest;
import com.shinhan.esg_be.domain.environment.dto.response.EnvironmentVerificationAvailabilityResponse;
import com.shinhan.esg_be.domain.environment.dto.response.EnvironmentVerificationResponse;
import com.shinhan.esg_be.domain.environment.entity.EnvironmentActivity;
import com.shinhan.esg_be.domain.environment.entity.UserEnvironmentActivity;
import com.shinhan.esg_be.domain.environment.entity.enums.EnvironmentActivityType;
import com.shinhan.esg_be.domain.environment.repository.EnvironmentActivityRepository;
import com.shinhan.esg_be.domain.environment.repository.UserEnvironmentActivityRepository;
import com.shinhan.esg_be.domain.environment.service.client.AzureDocumentIntelligenceClient;
import com.shinhan.esg_be.domain.environment.service.parser.ParsedEnvironmentData;
import com.shinhan.esg_be.domain.environment.service.parser.ReadAnalysisParser;
import com.shinhan.esg_be.domain.environment.service.parser.ReceiptAnalysisParser;
import com.shinhan.esg_be.domain.environment.service.validator.EvRentalVerificationValidator;
import com.shinhan.esg_be.domain.environment.service.validator.SharedBikeVerificationValidator;
import com.shinhan.esg_be.domain.environment.service.validator.TumblerVerificationValidator;
import com.shinhan.esg_be.domain.environment.service.validator.ValidationResult;
import com.shinhan.esg_be.domain.reward.service.RewardService;
import com.shinhan.esg_be.domain.reward.service.command.ApplyActivityRewardCommand;
import com.shinhan.esg_be.domain.reward.service.result.ApplyActivityRewardResult;
import com.shinhan.esg_be.domain.user.entity.User;
import com.shinhan.esg_be.domain.user.repository.UserRepository;
import com.shinhan.esg_be.global.common.enums.ActivityType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EnvironmentVerificationService {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    private static final long MAX_IMAGE_FILE_SIZE_BYTES = 10L * 1024L * 1024L;

    private final AzureDocumentIntelligenceClient azureDocumentIntelligenceClient;
    private final RewardService rewardService;
    private final UserRepository userRepository;
    private final EnvironmentActivityRepository environmentActivityRepository;
    private final UserEnvironmentActivityRepository userEnvironmentActivityRepository;
    private final ReceiptAnalysisParser receiptAnalysisParser;
    private final ReadAnalysisParser readAnalysisParser;
    private final TumblerVerificationValidator tumblerVerificationValidator;
    private final SharedBikeVerificationValidator sharedBikeVerificationValidator;
    private final EvRentalVerificationValidator evRentalVerificationValidator;

    public List<EnvironmentVerificationAvailabilityResponse> getVerificationAvailability() {
        User user = resolveCurrentUser();
        LocalDateTime startOfToday = getStartOfToday();
        LocalDateTime endOfToday = getEndOfToday();

        Map<EnvironmentActivityType, UserEnvironmentActivity> latestAttemptByType = new EnumMap<>(EnvironmentActivityType.class);

        for (UserEnvironmentActivity activity : userEnvironmentActivityRepository.findAllByUserAndCreatedAtBetween(
                user,
                startOfToday,
                endOfToday
        )) {
            EnvironmentActivityType activityType = resolveActivityTypeByName(activity.getActivity().getName());
            UserEnvironmentActivity currentLatest = latestAttemptByType.get(activityType);

            if (currentLatest == null || isLaterAttempt(activity, currentLatest)) {
                latestAttemptByType.put(activityType, activity);
            }
        }

        return Arrays.stream(EnvironmentActivityType.values())
                .map(activityType -> {
                    UserEnvironmentActivity latestAttempt = latestAttemptByType.get(activityType);

                    return new EnvironmentVerificationAvailabilityResponse(
                            activityType,
                            latestAttempt != null,
                            latestAttempt != null ? latestAttempt.getIsApproved() : null
                    );
                })
                .toList();
    }

    @Transactional
    public EnvironmentVerificationResponse createVerification(EnvironmentVerificationCreateRequest request) {
        MultipartFile image = request.getImage();
        validateImage(image);

        User user = resolveCurrentUser();
        EnvironmentActivityType activityType = resolveActivityType(request.getActivityType());
        EnvironmentActivity environmentActivity = resolveEnvironmentActivity(activityType);
        validateNotAttemptedToday(user, environmentActivity);

        String modelId = resolveModelId(activityType);
        JsonNode rawResult = analyze(image, modelId);
        ParsedEnvironmentData parsedData = parse(activityType, rawResult);
        logParsedData(activityType, parsedData);

        ValidationResult validationResult = validate(activityType, parsedData);
        log.info(
                "Environment verification result: activityType={}, approved={}, reason={}",
                activityType,
                validationResult.approved(),
                validationResult.reason()
        );

        UserEnvironmentActivity savedActivity = saveVerification(
                user,
                environmentActivity,
                parsedData,
                validationResult
        );
        int rewardPoint = applyReward(user, savedActivity, validationResult);

        return buildResponse(savedActivity.getActId(), activityType, validationResult.approved(), rewardPoint);
    }

    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "이미지 파일은 필수입니다.");
        }

        if (image.getSize() > MAX_IMAGE_FILE_SIZE_BYTES) {
            throw new ResponseStatusException(BAD_REQUEST, "사진 크기가 너무 큽니다.");
        }
    }

    private EnvironmentActivityType resolveActivityType(String activityType) {
        try {
            return EnvironmentActivityType.from(activityType);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(BAD_REQUEST, "지원하지 않는 활동 유형입니다.", e);
        }
    }

    private String resolveModelId(EnvironmentActivityType activityType) {
        return switch (activityType) {
            case TUMBLER -> "prebuilt-receipt";
            case SHARED_BIKE, EV_RENTAL -> "prebuilt-read";
        };
    }

    private JsonNode analyze(MultipartFile image, String modelId) {
        try {
            return azureDocumentIntelligenceClient.analyze(image.getBytes(), modelId);
        } catch (IOException e) {
            throw new ResponseStatusException(BAD_GATEWAY, "이미지 파일을 읽는 중 오류가 발생했습니다.", e);
        }
    }

    private ParsedEnvironmentData parse(EnvironmentActivityType activityType, JsonNode rawResult) {
        return switch (activityType) {
            case TUMBLER -> receiptAnalysisParser.parse(rawResult);
            case SHARED_BIKE, EV_RENTAL -> readAnalysisParser.parse(rawResult);
        };
    }

    private ValidationResult validate(EnvironmentActivityType activityType, ParsedEnvironmentData parsedData) {
        return switch (activityType) {
            case TUMBLER -> tumblerVerificationValidator.validate(parsedData);
            case SHARED_BIKE -> sharedBikeVerificationValidator.validate(parsedData);
            case EV_RENTAL -> evRentalVerificationValidator.validate(parsedData);
        };
    }

    private UserEnvironmentActivity saveVerification(
            User user,
            EnvironmentActivity environmentActivity,
            ParsedEnvironmentData parsedData,
            ValidationResult validationResult
    ) {
        UserEnvironmentActivity userEnvironmentActivity = UserEnvironmentActivity.create(
                user,
                environmentActivity,
                validationResult.approved(),
                parsedData.getRawText(),
                validationResult.approved() ? null : validationResult.reason()
        );

        return userEnvironmentActivityRepository.save(userEnvironmentActivity);
    }

    private int applyReward(
            User user,
            UserEnvironmentActivity savedActivity,
            ValidationResult validationResult
    ) {
        if (!validationResult.approved()) {
            return 0;
        }

        ApplyActivityRewardResult rewardResult = rewardService.applyActivityReward(
                new ApplyActivityRewardCommand(
                        user.getUserId(),
                        ActivityType.PHOTO,
                        null,
                        savedActivity.getCreatedAt() != null
                                ? savedActivity.getCreatedAt()
                                : LocalDateTime.now(KOREA_ZONE)
                )
        );

        return rewardResult.pointResult().activityPoint() + rewardResult.pointResult().bonusPoint();
    }

    private String resolveActivityName(EnvironmentActivityType activityType) {
        return switch (activityType) {
            case TUMBLER -> "텀블러 인증";
            case SHARED_BIKE -> "공유자전거 인증";
            case EV_RENTAL -> "전기차 인증";
        };
    }

    private EnvironmentActivityType resolveActivityTypeByName(String activityName) {
        return switch (activityName) {
            case "텀블러 인증" -> EnvironmentActivityType.TUMBLER;
            case "공유자전거 인증" -> EnvironmentActivityType.SHARED_BIKE;
            case "전기차 인증" -> EnvironmentActivityType.EV_RENTAL;
            default -> throw new ResponseStatusException(
                    INTERNAL_SERVER_ERROR,
                    "활동 목록 이름 매핑에 실패했습니다."
            );
        };
    }

    private User resolveCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ResponseStatusException(UNAUTHORIZED, "인증된 사용자 정보가 없습니다.");
        }

        return userRepository.findByLoginId(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        UNAUTHORIZED,
                        "인증된 사용자 정보를 찾을 수 없습니다."
                ));
    }

    private EnvironmentActivity resolveEnvironmentActivity(EnvironmentActivityType activityType) {
        return environmentActivityRepository.findByName(resolveActivityName(activityType))
                .orElseThrow(() -> new ResponseStatusException(
                        INTERNAL_SERVER_ERROR,
                        "활동 목록에 인증 대상 활동이 등록되어 있지 않습니다."
                ));
    }

    private void validateNotAttemptedToday(User user, EnvironmentActivity environmentActivity) {
        boolean attemptedToday = userEnvironmentActivityRepository.existsByUserAndActivityAndCreatedAtBetween(
                user,
                environmentActivity,
                getStartOfToday(),
                getEndOfToday()
        );

        if (attemptedToday) {
            throw new ResponseStatusException(CONFLICT, "오늘은 이미 해당 활동 인증을 시도했습니다.");
        }
    }

    private LocalDateTime getStartOfToday() {
        return LocalDate.now(KOREA_ZONE).atStartOfDay();
    }

    private LocalDateTime getEndOfToday() {
        return getStartOfToday().plusDays(1);
    }

    private boolean isLaterAttempt(UserEnvironmentActivity candidate, UserEnvironmentActivity current) {
        LocalDateTime candidateCreatedAt = candidate.getCreatedAt();
        LocalDateTime currentCreatedAt = current.getCreatedAt();

        if (candidateCreatedAt == null) {
            return false;
        }

        if (currentCreatedAt == null) {
            return true;
        }

        return candidateCreatedAt.isAfter(currentCreatedAt);
    }

    private EnvironmentVerificationResponse buildResponse(
            Long verificationId,
            EnvironmentActivityType activityType,
            boolean approved,
            int rewardPoint
    ) {
        return new EnvironmentVerificationResponse(
                verificationId,
                activityType,
                approved,
                rewardPoint
        );
    }

    private void logParsedData(EnvironmentActivityType activityType, ParsedEnvironmentData parsedData) {
        if (parsedData == null) {
            log.warn("Parsed environment data is null. activityType={}", activityType);
            return;
        }

        if (activityType == EnvironmentActivityType.SHARED_BIKE) {
            log.info(
                    "Parsed bike data: providerName={}, bikeQrId={}, completionText={}, rideTimeText={}, distanceText={}, paymentDateTimeText={}, paymentMethod={}, finalAmountText={}",
                    parsedData.getProviderName(),
                    parsedData.getBikeQrId(),
                    parsedData.getCompletionText(),
                    parsedData.getRideTimeText(),
                    parsedData.getDistanceText(),
                    parsedData.getPaymentDateTimeText(),
                    parsedData.getPaymentMethod(),
                    parsedData.getFinalAmountText()
            );
        }

        if (activityType == EnvironmentActivityType.TUMBLER) {
            log.info(
                    "Parsed tumbler data: merchantName={}, transactionDateText={}, transactionTimeText={}, totalAmountText={}",
                    parsedData.getMerchantName(),
                    parsedData.getTransactionDateText(),
                    parsedData.getTransactionTimeText(),
                    parsedData.getTotalAmountText()
            );
        }

        if (activityType == EnvironmentActivityType.EV_RENTAL) {
            log.info(
                    "Parsed EV data: completionText={}, vehicleModel={}, fuelType={}, carNumber={}, rentalPeriodText={}, pickupLocation={}, returnLocation={}, prepaidAmountText={}, rentalFeeText={}, insuranceFeeText={}",
                    parsedData.getCompletionText(),
                    parsedData.getVehicleModel(),
                    parsedData.getFuelType(),
                    parsedData.getCarNumber(),
                    parsedData.getRentalPeriodText(),
                    parsedData.getPickupLocation(),
                    parsedData.getReturnLocation(),
                    parsedData.getPrepaidAmountText(),
                    parsedData.getRentalFeeText(),
                    parsedData.getInsuranceFeeText()
            );
        }
    }
}
