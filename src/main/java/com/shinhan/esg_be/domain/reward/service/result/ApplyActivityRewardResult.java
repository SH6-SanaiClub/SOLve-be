package com.shinhan.esg_be.domain.reward.service.result;

import com.shinhan.esg_be.domain.point.service.result.ApplyActivityPointResult;
import com.shinhan.esg_be.domain.score.service.result.ApplyActivityScoreResult;

public record ApplyActivityRewardResult(
        ApplyActivityScoreResult scoreResult,
        ApplyActivityPointResult pointResult
) {
}
