package com.shinhan.esg_be.domain.point.service;

import com.shinhan.esg_be.domain.point.dto.response.MyPointHistoryItemResponse;
import com.shinhan.esg_be.domain.point.dto.response.MyPointHistoryResponse;
import com.shinhan.esg_be.domain.point.dto.response.MyPointSummaryResponse;
import com.shinhan.esg_be.domain.point.entity.UserPoint;
import com.shinhan.esg_be.domain.point.entity.enums.PointReason;
import com.shinhan.esg_be.domain.point.repository.UserPointRepository;
import com.shinhan.esg_be.domain.user.entity.User;
import com.shinhan.esg_be.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPointService {

    private final UserRepository userRepository;
    private final UserPointRepository userPointRepository;
    private final Clock clock;

    public MyPointSummaryResponse getSummary(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "사용자 정보를 찾을 수 없습니다."));

        return new MyPointSummaryResponse(
                user.getUserId(),
                user.getTotalPoints().longValue()
        );
    }

    public MyPointHistoryResponse getHistories(Long userId) {
        LocalDateTime startDateTime = LocalDateTime.now(clock).minusDays(30);

        List<MyPointHistoryItemResponse> histories = userPointRepository
                .findPointHistoriesByUserIdAndStartDateTime(userId, startDateTime)
                .stream()
                .map(this::toHistoryResponse)
                .toList();

        return new MyPointHistoryResponse(histories);
    }

    private MyPointHistoryItemResponse toHistoryResponse(UserPoint userPoint) {
        return new MyPointHistoryItemResponse(
                userPoint.getExchangeId(),
                resolveTitle(userPoint),
                userPoint.getReason(),
                userPoint.getChangedAmount(),
                userPoint.getPointAfter(),
                userPoint.getCreatedAt()
        );
    }

    private String resolveTitle(UserPoint userPoint) {
        return switch (userPoint.getReason()) {
            case DONATION -> "기부 참여";
            case VOLUNTEER -> "봉사 활동 참여";
            case VOLUNTEER_MILESTONE_BONUS -> "봉사 마일스톤 달성";
            case PURCHASE -> "친환경 제품 구매";
            case PHOTO -> "친환경 인증 참여";
            case PHOTO_STREAK_BONUS -> "친환경 인증 연속 참여";
            case QUIZ_CORRECT -> "오늘의 퀴즈 정답";
            case QUIZ_WRONG -> "오늘의 퀴즈 참여";
            case QUIZ_MONTHLY_BONUS -> "퀴즈 월간 보너스";
            case ABUSE_RECLAIM -> "부정 적립 회수";
            case EXCHANGE -> resolveExchangeTitle(userPoint);
        };
    }

    private String resolveExchangeTitle(UserPoint userPoint) {
        if (userPoint.getItem() == null) {
            return "상품 교환";
        }

        return userPoint.getItem().getName();
    }
}
