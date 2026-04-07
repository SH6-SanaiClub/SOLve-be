package com.shinhan.esg_be.domain.point.service.command;

import com.shinhan.esg_be.domain.point.entity.enums.PointReason;

import java.math.BigDecimal;

public record PointCalculationCommand(
        PointReason pointReason,
        Integer fixedPointValue,
        BigDecimal pointRate,
        BigDecimal amount
) {
}
