package com.shinhan.esg_be.domain.environment.service.validator;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ValidationDateUtils {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    private static final Pattern YYYY_MM_DD_PATTERN =
            Pattern.compile("(?<!\\d)(\\d{4})[./-](\\d{1,2})[./-](\\d{1,2})(?!\\d)");
    private static final Pattern YY_MM_DD_PATTERN =
            Pattern.compile("(?<!\\d)(\\d{2})[./-](\\d{1,2})[./-](\\d{1,2})(?!\\d)");
    private static final Pattern MM_DD_PATTERN =
            Pattern.compile("(?<!\\d)(\\d{1,2})[./-](\\d{1,2})(?![:\\d])");

    private ValidationDateUtils() {
    }

    static boolean isToday(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        LocalDate extractedDate = extractDate(value);
        return extractedDate != null && extractedDate.equals(LocalDate.now(KOREA_ZONE));
    }

    private static LocalDate extractDate(String value) {
        Matcher yyyyMatcher = YYYY_MM_DD_PATTERN.matcher(value);
        if (yyyyMatcher.find()) {
            return buildDate(
                    Integer.parseInt(yyyyMatcher.group(1)),
                    Integer.parseInt(yyyyMatcher.group(2)),
                    Integer.parseInt(yyyyMatcher.group(3))
            );
        }

        Matcher yyMatcher = YY_MM_DD_PATTERN.matcher(value);
        if (yyMatcher.find()) {
            return buildDate(
                    2000 + Integer.parseInt(yyMatcher.group(1)),
                    Integer.parseInt(yyMatcher.group(2)),
                    Integer.parseInt(yyMatcher.group(3))
            );
        }

        Matcher mmDdMatcher = MM_DD_PATTERN.matcher(value);
        if (mmDdMatcher.find()) {
            return buildDate(
                    LocalDate.now(KOREA_ZONE).getYear(),
                    Integer.parseInt(mmDdMatcher.group(1)),
                    Integer.parseInt(mmDdMatcher.group(2))
            );
        }

        return null;
    }

    private static LocalDate buildDate(int year, int month, int day) {
        try {
            return LocalDate.of(year, month, day);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
