package com.shinhan.esg_be.domain.point.service.command;

import com.shinhan.esg_be.domain.point.entity.enums.PointCategory;

import java.math.BigDecimal;

public record PointCalculationCommand(
        PointCategory pointCategory,
        Integer fixedPointValue,
        BigDecimal pointRate,
        BigDecimal amount
) {
}
