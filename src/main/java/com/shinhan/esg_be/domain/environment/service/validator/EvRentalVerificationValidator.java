package com.shinhan.esg_be.domain.environment.service.validator;

import com.shinhan.esg_be.domain.environment.service.parser.ParsedEnvironmentData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Component
@Slf4j
public class EvRentalVerificationValidator {

    private static final String NOT_EV_REASON =
            "전기차 이용내역이 아닙니다.";
    private static final String NOT_TODAY_REASON =
            "당일 이용내역만 인증 가능합니다.";

    private static final Pattern RENTAL_PERIOD_PATTERN =
            Pattern.compile(
                    "(\\d{1,2}[./-]\\d{1,2}.*\\d{1,2}:\\d{2}\\s*~\\s*\\d{1,2}:\\d{2})"
                            + "|(\\d{1,2}[./-]\\d{1,2}.*\\d{1,2}:\\d{2})"
                            + "|(\\d+\\s*시간)",
                    Pattern.CASE_INSENSITIVE
            );
    private static final Pattern CAR_NUMBER_PATTERN =
            Pattern.compile("\\d{2,3}[가-힣]\\d{4}");
    private static final Pattern AMOUNT_PATTERN =
            Pattern.compile("(\\d{1,3}(,\\d{3})*|\\d+)\\s*원");

    private static final List<String> EV_KEYWORDS = List.of(
            "전기",
            "전기차",
            "ev",
            "bev"
    );

    private static final List<String> NON_EV_KEYWORDS = List.of(
            "가솔린",
            "디젤",
            "휘발유",
            "lpg",
            "하이브리드",
            "hev",
            "phev"
    );

    // Final approval check.
    public ValidationResult validate(ParsedEnvironmentData parsedData) {
        if (parsedData == null) {
            log.info("EV validation failed: parsedData is null");
            return ValidationResult.rejected(NOT_EV_REASON);
        }

        boolean isToday = ValidationDateUtils.isToday(parsedData.getRentalPeriodText());
        if (!isToday) {
            log.info("EV validation failed: rentalPeriodText is not today. rentalPeriodText={}", parsedData.getRentalPeriodText());
            return ValidationResult.rejected(NOT_TODAY_REASON);
        }

        boolean nonEvSignal = hasNonEvSignal(parsedData);
        if (nonEvSignal) {
            log.info("EV validation failed: detected non-EV signal. fuelType={}, rawTextContainsNonEv=true", parsedData.getFuelType());
            return ValidationResult.rejected(NOT_EV_REASON);
        }

        boolean completion = hasCompletion(parsedData);
        boolean vehicleInfo = hasVehicleInfo(parsedData);
        boolean evSignal = hasEvSignal(parsedData);
        boolean rentalPeriodMatched = matches(parsedData.getRentalPeriodText(), RENTAL_PERIOD_PATTERN);
        boolean pickup = hasText(parsedData.getPickupLocation());
        boolean returned = hasText(parsedData.getReturnLocation());
        boolean amount = hasAnyAmount(parsedData);

        boolean approved = completion
                && vehicleInfo
                && evSignal
                && rentalPeriodMatched
                && pickup
                && returned
                && amount;

        log.info(
                "EV validation checks: completion={}, vehicleInfo={}, evSignal={}, rentalPeriodMatched={}, pickup={}, returnLocation={}, amount={}, rentalPeriodText={}, fuelType={}, vehicleModel={}, carNumber={}, pickupLocation={}, returnLocationText={}, prepaidAmountText={}, rentalFeeText={}, insuranceFeeText={}",
                completion,
                vehicleInfo,
                evSignal,
                rentalPeriodMatched,
                pickup,
                returned,
                amount,
                parsedData.getRentalPeriodText(),
                parsedData.getFuelType(),
                parsedData.getVehicleModel(),
                parsedData.getCarNumber(),
                parsedData.getPickupLocation(),
                parsedData.getReturnLocation(),
                parsedData.getPrepaidAmountText(),
                parsedData.getRentalFeeText(),
                parsedData.getInsuranceFeeText()
        );

        if (!approved) {
            return ValidationResult.rejected(NOT_EV_REASON);
        }

        return ValidationResult.success();
    }

    // Check whether the rental has been completed.
    private boolean hasCompletion(ParsedEnvironmentData parsedData) {
        String completionText = parsedData.getCompletionText();
        if (!hasText(completionText)) {
            return false;
        }

        return completionText.contains("반납완료")
                || completionText.contains("반납 완료")
                || completionText.contains("이용완료")
                || completionText.contains("이용 완료")
                || completionText.contains("성공");
    }

    private boolean hasVehicleInfo(ParsedEnvironmentData parsedData) {
        return hasText(parsedData.getVehicleModel())
                && matches(parsedData.getCarNumber(), CAR_NUMBER_PATTERN);
    }

    private boolean hasEvSignal(ParsedEnvironmentData parsedData) {
        if (containsAny(parsedData.getFuelType(), EV_KEYWORDS)) {
            return true;
        }

        return containsAny(buildSearchText(parsedData), EV_KEYWORDS);
    }

    private boolean hasNonEvSignal(ParsedEnvironmentData parsedData) {
        if (containsAny(parsedData.getFuelType(), NON_EV_KEYWORDS)) {
            return true;
        }

        return containsAny(buildSearchText(parsedData), NON_EV_KEYWORDS);
    }

    private boolean hasAnyAmount(ParsedEnvironmentData parsedData) {
        return matches(parsedData.getPrepaidAmountText(), AMOUNT_PATTERN)
                || matches(parsedData.getRentalFeeText(), AMOUNT_PATTERN)
                || matches(parsedData.getInsuranceFeeText(), AMOUNT_PATTERN);
    }

    private String buildSearchText(ParsedEnvironmentData parsedData) {
        StringBuilder builder = new StringBuilder();
        append(builder, parsedData.getRawText());
        append(builder, parsedData.getFuelType());
        append(builder, parsedData.getVehicleModel());

        if (parsedData.getLines() != null) {
            for (String line : parsedData.getLines()) {
                append(builder, line);
            }
        }

        return builder.toString().toLowerCase();
    }

    private boolean containsAny(String source, List<String> keywords) {
        if (!hasText(source)) {
            return false;
        }

        String normalized = source.toLowerCase();
        for (String keyword : keywords) {
            if (normalized.contains(keyword.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    private boolean matches(String value, Pattern pattern) {
        return hasText(value) && pattern.matcher(value).find();
    }

    private void append(StringBuilder builder, String value) {
        if (hasText(value)) {
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(value);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
