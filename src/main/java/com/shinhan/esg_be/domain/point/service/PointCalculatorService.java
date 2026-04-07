package com.shinhan.esg_be.domain.point.service;

import com.shinhan.esg_be.domain.point.service.command.PointCalculationCommand;
import com.shinhan.esg_be.domain.point.service.result.PointCalculationResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class PointCalculatorService {

    public PointCalculationResult calculateActivity(PointCalculationCommand command, int currentPoint) {
        if (command == null) {
            throw new IllegalArgumentException("PointCalculationCommand must not be null.");
        }

        int appliedPoint = calculatePoint(command.fixedPointValue(), command.pointRate(), command.amount());
        return new PointCalculationResult(
                command.pointReason(),
                appliedPoint,
                currentPoint + appliedPoint
        );
    }

    private int calculatePoint(Integer fixedPointValue, BigDecimal pointRate, BigDecimal amount) {
        if (fixedPointValue != null && fixedPointValue > 0) {
            return fixedPointValue;
        }
        if (pointRate != null && amount != null && pointRate.compareTo(BigDecimal.ZERO) > 0) {
            return amount.multiply(pointRate)
                    .setScale(0, RoundingMode.HALF_UP)
                    .intValue();
        }
        return 0;
    }
}
