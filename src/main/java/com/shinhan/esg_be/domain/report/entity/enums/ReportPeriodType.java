package com.shinhan.esg_be.domain.report.entity.enums;

import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

public enum ReportPeriodType {
    THREE_MONTHS("3M", "최근 3개월", 3),
    SIX_MONTHS("6M", "최근 6개월", 6),
    ONE_YEAR("1Y", "최근 1년", 12);

    private final String code;
    private final String label;
    private final int months;

    ReportPeriodType(String code, String label, int months) {
        this.code = code;
        this.label = label;
        this.months = months;
    }

    public static ReportPeriodType from(String code) {
        for (ReportPeriodType value : values()) {
            if (value.code.equalsIgnoreCase(code)) {
                return value;
            }
        }
        throw new ResponseStatusException(BAD_REQUEST, "Invalid periodType.");
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public LocalDateTime startDateTime(LocalDate referenceDate) {
        return referenceDate.minusMonths(months).atStartOfDay();
    }

    public LocalDateTime endDateTime(LocalDate referenceDate) {
        return referenceDate.plusDays(1).atStartOfDay();
    }
}
