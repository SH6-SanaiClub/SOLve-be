package com.shinhan.esg_be.domain.score.service.result;

import com.shinhan.esg_be.global.common.enums.ScoreReason;

public record PenaltyApplicationResult(
        ScoreReason reason,
        int appliedEReduction,
        int appliedSReduction,
        int appliedGActivityReduction,
        boolean loanBlocked,
        boolean applied
) {
}
