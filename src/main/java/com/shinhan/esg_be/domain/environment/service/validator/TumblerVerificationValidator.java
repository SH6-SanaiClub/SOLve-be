package com.shinhan.esg_be.domain.environment.service.validator;

import com.shinhan.esg_be.domain.environment.service.parser.ParsedEnvironmentData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class TumblerVerificationValidator {

    private static final String NOT_TODAY_RECEIPT_REASON =
            "당일 영수증만 인증 가능합니다.";
    private static final String NO_DISCOUNT_REASON =
            "할인 내역이 확인되지 않았습니다.";

    private static final Pattern AMOUNT_PATTERN =
            Pattern.compile("(\\d{1,3}(,\\d{3})*|\\d+)(\\s*원)?");
    private static final Pattern NEGATIVE_DISCOUNT_PATTERN =
            Pattern.compile("-\\s*(\\d{1,3}(,\\d{3})*|\\d+)(\\s*원)?");
    private static final Pattern POSITIVE_DISCOUNT_PATTERN =
            Pattern.compile("(\\d{1,3}(,\\d{3})*|\\d+)(\\s*원)?");

    private static final List<String> TUMBLER_KEYWORDS = List.of(
            "개인컵",
            "개인 컵",
            "텀블러",
            "다회용컵",
            "다회용 컵",
            "리유저블컵",
            "리유저블 컵"
    );

    // Final approval check.
    public ValidationResult validate(ParsedEnvironmentData parsedData) {
        if (parsedData == null) {
            log.info("Tumbler validation failed: parsedData is null");
            return ValidationResult.rejected(NO_DISCOUNT_REASON);
        }

        boolean isToday = ValidationDateUtils.isToday(parsedData.getTransactionDateText());
        if (!isToday) {
            log.info(
                    "Tumbler validation failed: transactionDateText is not today. transactionDateText={}",
                    parsedData.getTransactionDateText()
            );
            return ValidationResult.rejected(NOT_TODAY_RECEIPT_REASON);
        }

        boolean merchant = hasText(parsedData.getMerchantName());
        boolean transactionTime = hasText(parsedData.getTransactionTimeText());
        boolean amount = hasAmount(parsedData);
        boolean tumblerSignal = hasTumblerSignal(parsedData);
        boolean discountApplied = hasDiscountApplied(parsedData);

        boolean approved = merchant
                && transactionTime
                && amount
                && tumblerSignal
                && discountApplied;

        log.info(
                "Tumbler validation checks: merchant={}, transactionTime={}, amount={}, tumblerSignal={}, discountApplied={}, merchantName={}, transactionDateText={}, transactionTimeText={}, totalAmountText={}",
                merchant,
                transactionTime,
                amount,
                tumblerSignal,
                discountApplied,
                parsedData.getMerchantName(),
                parsedData.getTransactionDateText(),
                parsedData.getTransactionTimeText(),
                parsedData.getTotalAmountText()
        );

        if (!approved) {
            return ValidationResult.rejected(NO_DISCOUNT_REASON);
        }

        return ValidationResult.success();
    }

    private boolean hasTumblerSignal(ParsedEnvironmentData parsedData) {
        String searchText = buildSearchText(parsedData);

        for (String keyword : TUMBLER_KEYWORDS) {
            if (searchText.contains(keyword.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    private boolean hasDiscountApplied(ParsedEnvironmentData parsedData) {
        List<String> lines = parsedData.getLines();
        if (lines != null) {
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);

                if (!containsTumblerKeyword(line)) {
                    continue;
                }

                if (hasPositiveDiscountAmount(line)) {
                    return true;
                }

                if (i + 1 < lines.size() && hasPositiveDiscountAmount(lines.get(i + 1))) {
                    return true;
                }
            }
        }

        String rawText = parsedData.getRawText();
        return containsTumblerKeyword(rawText) && hasPositiveDiscountAmount(rawText);
    }

    private boolean containsTumblerKeyword(String value) {
        if (!hasText(value)) {
            return false;
        }

        String normalized = value.toLowerCase();
        for (String keyword : TUMBLER_KEYWORDS) {
            if (normalized.contains(keyword.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    private boolean hasPositiveDiscountAmount(String value) {
        if (!hasText(value)) {
            return false;
        }

        Matcher negativeMatcher = NEGATIVE_DISCOUNT_PATTERN.matcher(value);
        while (negativeMatcher.find()) {
            if (parseAmount(negativeMatcher.group(1)) > 0) {
                return true;
            }
        }

        if (!value.contains("할인")) {
            return false;
        }

        Matcher positiveMatcher = POSITIVE_DISCOUNT_PATTERN.matcher(value);
        while (positiveMatcher.find()) {
            if (parseAmount(positiveMatcher.group(1)) > 0) {
                return true;
            }
        }

        return false;
    }

    private boolean hasAmount(ParsedEnvironmentData parsedData) {
        if (matches(parsedData.getTotalAmountText(), AMOUNT_PATTERN)) {
            return true;
        }

        List<String> lines = parsedData.getLines();
        if (lines != null) {
            for (String line : lines) {
                if (line.contains("결제금액") && matches(line, AMOUNT_PATTERN)) {
                    return true;
                }
            }
        }

        return false;
    }

    private String buildSearchText(ParsedEnvironmentData parsedData) {
        StringBuilder builder = new StringBuilder();
        append(builder, parsedData.getRawText());

        if (parsedData.getLines() != null) {
            for (String line : parsedData.getLines()) {
                append(builder, line);
            }
        }

        return builder.toString().toLowerCase();
    }

    private int parseAmount(String value) {
        try {
            return Integer.parseInt(value.replace(",", "").trim());
        } catch (RuntimeException e) {
            return 0;
        }
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
