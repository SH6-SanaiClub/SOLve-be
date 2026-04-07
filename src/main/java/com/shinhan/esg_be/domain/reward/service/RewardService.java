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

    /**
     * 활동이 최종 확정된 시점에 이 메서드 하나만 호출하면 점수와 포인트가 함께 반영됩니다.
     *
     * 호출 예시
     * {@code
     * ApplyActivityRewardResult result = rewardService.applyActivityReward(
     *         new ApplyActivityRewardCommand(
     *                 userId,
     *                 ActivityType.DONATION,
     *                 BigDecimal.valueOf(30000),
     *                 LocalDateTime.now()
     *         )
     * );
     * }
     *
     * 사용 예시
     * - 기부 완료 후: DONATION + 실제 기부 금액
     * - 친환경 상품 구매 완료 후: PURCHASE + 실제 결제 금액
     * - 봉사 완료 후: VOLUNTEER + null
     * - 사진 인증 완료 후: PHOTO + null
     * - 퀴즈 정답/오답 처리 후: QUIZ_CORRECT 또는 QUIZ_WRONG + null
     * - 대출 상환 완료 후: LOAN_REPAY + null
     *
     * amount
     * - 금액 비례 포인트 계산이 필요한 활동만 실제 금액을 넘깁니다.
     * - 금액이 없는 활동은 null을 넘기면 됩니다.
     *
     * activityDateTime
     * - 실제 활동 발생 시각을 넘겨주세요.
     * - 연속 참여, 월간 보너스 같은 날짜 기준 로직에 사용됩니다.
     */

    @Transactional
    public ApplyActivityRewardResult applyActivityReward(ApplyActivityRewardCommand command) {
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
