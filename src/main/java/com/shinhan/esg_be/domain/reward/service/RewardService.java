package com.shinhan.esg_be.domain.reward.service;

import com.shinhan.esg_be.domain.point.service.PointService;
import com.shinhan.esg_be.domain.point.service.command.ApplyActivityPointCommand;
import com.shinhan.esg_be.domain.point.service.result.ApplyActivityPointResult;
import com.shinhan.esg_be.domain.reward.service.command.ApplyActivityRewardCommand;
import com.shinhan.esg_be.domain.reward.service.result.ApplyActivityRewardResult;
import com.shinhan.esg_be.domain.score.service.ScoreService;
import com.shinhan.esg_be.domain.score.service.command.ApplyActivityScoreCommand;
import com.shinhan.esg_be.domain.score.service.result.ApplyActivityScoreResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RewardService {

    private final ScoreService scoreService;
    private final PointService pointService;

    @Transactional
    public ApplyActivityRewardResult applyActivityReward(ApplyActivityRewardCommand command) {
        // Teams can use this single entrypoint to apply score and point together.
        ApplyActivityScoreResult scoreResult = scoreService.applyActivityScore(
                new ApplyActivityScoreCommand(
                        command.userId(),
                        command.activityType(),
                        command.amount(),
                        command.activityDateTime()
                )
        );

        ApplyActivityPointResult pointResult = pointService.applyActivityPoint(
                new ApplyActivityPointCommand(
                        command.userId(),
                        command.activityType(),
                        command.amount(),
                        command.activityDateTime(),
                        null
                )
        );

        return new ApplyActivityRewardResult(scoreResult, pointResult);
    }
}
