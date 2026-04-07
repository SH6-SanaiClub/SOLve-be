package com.shinhan.esg_be.domain.environment.service.validator;

import com.shinhan.esg_be.domain.environment.service.parser.ParsedEnvironmentData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Component
@Slf4j
public class SharedBikeVerificationValidator {

    private static final String NOT_SHARED_BIKE_REASON = "공유자전거 이용내역이 아닙니다.";
    private static final String NOT_TODAY_REASON = "당일 이용내역만 인증 가능합니다.";

    private static final Pattern RIDE_TIME_PATTERN = Pattern.compile("\\d+\\s*분");
    private static final Pattern DISTANCE_PATTERN =
            Pattern.compile("\\d+(\\.\\d+)?\\s*(km|m)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PAYMENT_DATETIME_PATTERN =
            Pattern.compile("\\d{2,4}[./-]\\d{1,2}[./-]\\d{1,2}\\s+\\d{1,2}:\\d{2}");
    private static final Pattern AMOUNT_PATTERN =
            Pattern.compile("(\\d{1,3}(,\\d{3})*|\\d+)\\s*원");

    private static final List<String> BIKE_KEYWORDS = List.of(
            "자전거",
            "공유자전거",
            "바이크",
            "전동 바이크",
            "따릉",
            "따릉이",
            "싱글기어",
            "bike",
            "bicycle"
    );

    private static final List<String> KICKBOARD_KEYWORDS = List.of(
            "킥보드",
            "전동킥보드",
            "kickboard",
            "scooter",
            "킥고잉",
            "지쿠터",
            "beam",
            "deer"
    );

    public ValidationResult validate(ParsedEnvironmentData parsedData) {
        if (parsedData == null) {
            log.info("Shared bike validation failed: parsedData is null");
            return ValidationResult.rejected(NOT_SHARED_BIKE_REASON);
        }

        boolean isToday = ValidationDateUtils.isToday(parsedData.getPaymentDateTimeText());
        if (!isToday) {
            log.info(
                    "Shared bike validation failed: paymentDateTimeText is not today. paymentDateTimeText={}",
                    parsedData.getPaymentDateTimeText()
            );
            return ValidationResult.rejected(NOT_TODAY_REASON);
        }

        if (hasKickboardSignal(parsedData)) {
            log.info("Shared bike validation failed: detected kickboard signal");
            return ValidationResult.rejected(NOT_SHARED_BIKE_REASON);
        }

        boolean bikeSignal = hasBikeSignal(parsedData);
        boolean completion = hasCompletion(parsedData);
        boolean rideTime = matches(parsedData.getRideTimeText(), RIDE_TIME_PATTERN);
        boolean distance = matches(parsedData.getDistanceText(), DISTANCE_PATTERN);
        boolean paymentDateTime = matches(parsedData.getPaymentDateTimeText(), PAYMENT_DATETIME_PATTERN);
        boolean amount = hasAmountEvidence(parsedData);

        boolean approved = bikeSignal
                && completion
                && rideTime
                && distance
                && paymentDateTime
                && amount;

        log.info(
                "Shared bike validation checks: bikeSignal={}, completion={}, rideTime={}, distance={}, paymentDateTime={}, amount={}, providerName={}, bikeQrId={}, completionText={}, rideTimeText={}, distanceText={}, paymentDateTimeText={}, finalAmountText={}",
                bikeSignal,
                completion,
                rideTime,
                distance,
                paymentDateTime,
                amount,
                parsedData.getProviderName(),
                parsedData.getBikeQrId(),
                parsedData.getCompletionText(),
                parsedData.getRideTimeText(),
                parsedData.getDistanceText(),
                parsedData.getPaymentDateTimeText(),
                parsedData.getFinalAmountText()
        );

        if (!approved) {
            return ValidationResult.rejected(NOT_SHARED_BIKE_REASON);
        }

        return ValidationResult.success();
    }

    private boolean hasBikeSignal(ParsedEnvironmentData parsedData) {
        if (hasText(parsedData.getBikeQrId()) || hasText(parsedData.getProviderName())) {
            return true;
        }

        String searchText = buildSearchText(parsedData);
        return containsAny(searchText, BIKE_KEYWORDS)
                || searchText.contains("칼로리")
                || searchText.contains("qr ");
    }

    private boolean hasKickboardSignal(ParsedEnvironmentData parsedData) {
        return containsAny(buildSearchText(parsedData), KICKBOARD_KEYWORDS);
    }

    private boolean hasCompletion(ParsedEnvironmentData parsedData) {
        String completionText = parsedData.getCompletionText();
        if (hasText(completionText)) {
            return completionText.contains("반납완료")
                    || completionText.contains("반납 완료")
                    || completionText.contains("주차 반납 인증")
                    || completionText.contains("반납 인증")
                    || completionText.contains("이용완료")
                    || completionText.contains("이용 완료")
                    || completionText.contains("성공");
        }

        String searchText = buildSearchText(parsedData);
        return searchText.contains("거래확인증")
                || searchText.contains("이용 상세정보")
                || searchText.contains("이용 상세")
                || searchText.contains("이용내역")
                || (searchText.contains("출발") && searchText.contains("도착"))
                || (searchText.contains("대여장소") && searchText.contains("반납장소"));
    }

    private boolean hasAmountEvidence(ParsedEnvironmentData parsedData) {
        if (matches(parsedData.getFinalAmountText(), AMOUNT_PATTERN)) {
            return true;
        }

        String searchText = buildSearchText(parsedData);
        return searchText.contains("대여시간")
                && searchText.contains("반납시간")
                && searchText.contains("대여장소")
                && searchText.contains("반납장소");
    }

    private String buildSearchText(ParsedEnvironmentData parsedData) {
        StringBuilder builder = new StringBuilder();
        append(builder, parsedData.getRawText());
        append(builder, parsedData.getProviderName());
        append(builder, parsedData.getCompletionText());

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
