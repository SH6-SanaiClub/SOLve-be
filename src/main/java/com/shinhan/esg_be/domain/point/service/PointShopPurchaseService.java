package com.shinhan.esg_be.domain.point.service;

import com.shinhan.esg_be.domain.point.dto.response.PointShopPurchaseResponse;
import com.shinhan.esg_be.domain.point.entity.Item;
import com.shinhan.esg_be.domain.point.entity.UserPoint;
import com.shinhan.esg_be.domain.point.entity.enums.PointReason;
import com.shinhan.esg_be.domain.point.repository.UserPointRepository;
import com.shinhan.esg_be.domain.user.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class PointShopPurchaseService {

    private static final String EXCHANGE_CODE_PREFIX = "SOLV-";
    private static final int EXCHANGE_CODE_LENGTH = 8;
    private static final int EXCHANGE_CODE_RETRY_COUNT = 10;

    private final EntityManager entityManager;
    private final UserPointRepository userPointRepository;

    @Transactional
    public PointShopPurchaseResponse purchase(Long userId, Long itemId) {
        User user = getUserForUpdate(userId);
        Item item = getItemForUpdate(itemId);

        validatePurchasable(user, item);

        long usedPoints = item.getRequiredPoints();
        user.applyPoint((int) -usedPoints);
        item.decreaseStock();

        String exchangeCode = generateExchangeCode();
        UserPoint savedUserPoint = userPointRepository.saveAndFlush(
                UserPoint.create(
                        user,
                        item,
                        PointReason.EXCHANGE,
                        -usedPoints,
                        user.getTotalPoints().longValue(),
                        exchangeCode
                )
        );

        return new PointShopPurchaseResponse(
                item.getItemId(),
                item.getName(),
                usedPoints,
                user.getTotalPoints().longValue(),
                exchangeCode,
                savedUserPoint.getCreatedAt()
        );
    }

    private User getUserForUpdate(Long userId) {
        User user = entityManager.find(User.class, userId, LockModeType.PESSIMISTIC_WRITE);
        if (user == null) {
            throw new ResponseStatusException(NOT_FOUND, "사용자 정보를 찾을 수 없습니다.");
        }
        return user;
    }

    private Item getItemForUpdate(Long itemId) {
        Item item = entityManager.find(Item.class, itemId, LockModeType.PESSIMISTIC_WRITE);
        if (item == null || !Boolean.TRUE.equals(item.getIsActive())) {
            throw new ResponseStatusException(NOT_FOUND, "상품 정보를 찾을 수 없습니다.");
        }
        return item;
    }

    private void validatePurchasable(User user, Item item) {
        if (item.getStock() == null || item.getStock() <= 0) {
            throw new ResponseStatusException(CONFLICT, "품절된 상품입니다.");
        }

        long totalPoints = user.getTotalPoints().longValue();
        if (totalPoints < item.getRequiredPoints()) {
            throw new ResponseStatusException(BAD_REQUEST, "포인트가 부족합니다.");
        }
    }

    private String generateExchangeCode() {
        for (int attempt = 0; attempt < EXCHANGE_CODE_RETRY_COUNT; attempt++) {
            String exchangeCode = EXCHANGE_CODE_PREFIX + UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, EXCHANGE_CODE_LENGTH)
                    .toUpperCase(Locale.ROOT);

            if (!userPointRepository.existsByExchangeCode(exchangeCode)) {
                return exchangeCode;
            }
        }

        throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "교환 코드 생성에 실패했습니다.");
    }
}
