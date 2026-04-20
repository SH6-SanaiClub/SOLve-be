package com.shinhan.esg_be.domain.bank.service;

import com.shinhan.esg_be.domain.bank.entity.FinancialProduct;
import com.shinhan.esg_be.domain.user.entity.User;

import java.util.Arrays;
import java.util.Optional;

enum SavingsProductKind {
    GREEN_STEP_UP("그린 스텝업"),
    EARTH_GUARDIAN("지구 수호대"),
    WARM_COMPANION("따뜻한 동행"),
    SMART_FINANCE("바른 금융 스마트"),
    ESG_MASTER("ESG 마스터");

    static final int ESG_MASTER_MIN_TOTAL_SCORE = 900;
    static final int GREEN_STEP_UP_SCORE_STEP = 40;
    static final int GREEN_STEP_UP_MAX_ADDED_RATE_BPS = 240;
    static final int WARM_COMPANION_MONTHLY_INCREMENT_BPS = 30;
    static final int SMART_FINANCE_MONTHLY_INCREMENT_BPS = 10;
    static final int SMART_FINANCE_MAINTAIN_BONUS_BPS = 30;
    static final int SMART_FINANCE_NO_PENALTY_BONUS_BPS = 100;

    private final String keyword;

    SavingsProductKind(String keyword) {
        this.keyword = keyword;
    }

    boolean matches(FinancialProduct product) {
        String productName = product.getName();
        return productName != null && productName.contains(keyword);
    }

    boolean isEligible(User user) {
        if (this == ESG_MASTER) {
            return user.getTotalScore() >= ESG_MASTER_MIN_TOTAL_SCORE;
        }
        return true;
    }

    static Optional<SavingsProductKind> from(FinancialProduct product) {
        return Arrays.stream(values())
                .filter(kind -> kind.matches(product))
                .findFirst();
    }
}
