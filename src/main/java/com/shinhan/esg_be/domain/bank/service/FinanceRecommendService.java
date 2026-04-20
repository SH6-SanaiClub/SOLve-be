package com.shinhan.esg_be.domain.bank.service;

import com.shinhan.esg_be.domain.bank.dto.response.SavingsRecommendResponse;
import com.shinhan.esg_be.domain.bank.entity.FinancialProduct;
import com.shinhan.esg_be.domain.bank.entity.enums.ProductType;
import com.shinhan.esg_be.domain.bank.entity.enums.SavingStatus;
import com.shinhan.esg_be.domain.bank.repository.FinancialProductRepository;
import com.shinhan.esg_be.domain.bank.repository.UserSavingRepository;
import com.shinhan.esg_be.domain.stat.repository.UserMonthlyStatRepository;
import com.shinhan.esg_be.domain.user.entity.User;
import com.shinhan.esg_be.domain.user.repository.UserRepository;
import com.shinhan.esg_be.global.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FinanceRecommendService {

    private final FinancialProductRepository financialProductRepository;
    private final UserSavingRepository userSavingRepository;
    private final UserMonthlyStatRepository userMonthlyStatRepository;
    private final UserRepository userRepository;
    private final FinanceRecommendFeatureExtractor featureExtractor;
    private final SavingsProductScorer savingsProductScorer;

    public SavingsRecommendResponse recommend(String loginId) {
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new BadRequestException("사용자를 찾을 수 없습니다."));

        List<FinancialProduct> availableProducts = getAvailableSavings(user);
        if (availableProducts.isEmpty()) {
            return buildNoAvailableResponse();
        }

        long activityMonths = userMonthlyStatRepository.countByUser_UserId(user.getUserId());
        if (activityMonths == 0) {
            return buildNewUserResponse(availableProducts);
        }

        FinanceRecommendFeature feature = featureExtractor.extract(user);
        if (!feature.hasRecentActivity()) {
            return buildLowConfidenceResponse(availableProducts);
        }

        SavingsProductScorer.ScoredProduct best = availableProducts.stream()
                .map(product -> savingsProductScorer.score(product, feature))
                .max(Comparator.comparingInt(SavingsProductScorer.ScoredProduct::getMatchScore))
                .orElseGet(() -> buildGenericFallback(availableProducts.get(0)));

        return SavingsRecommendResponse.builder()
                .isNewUser(false)
                .recommendation(toResponse(best, false))
                .build();
    }

    private List<FinancialProduct> getAvailableSavings(User user) {
        Set<Long> activeSavingProductIds = userSavingRepository.findAllByUser_UserIdAndStatus(user.getUserId(), SavingStatus.ACTIVE)
                .stream()
                .map(userSaving -> userSaving.getFinancialProduct().getFinProductId())
                .collect(Collectors.toSet());

        return financialProductRepository.findByTypeAndIsActiveTrue(ProductType.SAVINGS)
                .stream()
                .filter(product -> !activeSavingProductIds.contains(product.getFinProductId()))
                .filter(product -> SavingsProductKind.from(product)
                        .map(kind -> kind.isEligible(user))
                        .orElse(true))
                .toList();
    }

    private SavingsRecommendResponse buildNewUserResponse(List<FinancialProduct> availableProducts) {
        FinancialProduct recommendedProduct = availableProducts.stream()
                .filter(product -> SavingsProductKind.from(product).orElse(null) == SavingsProductKind.GREEN_STEP_UP)
                .findFirst()
                .orElse(availableProducts.get(0));

        return SavingsRecommendResponse.builder()
                .isNewUser(true)
                .recommendation(SavingsRecommendResponse.RecommendItem.builder()
                        .productId(recommendedProduct.getFinProductId())
                        .productName(recommendedProduct.getName())
                        .matchScore(0)
                        .expectedMaxRate(formatRateLabel(recommendedProduct))
                        .reason("활동 데이터가 아직 충분하지 않아 범용형 적금부터 안내해드려요.")
                        .actionable("먼저 꾸준히 활동을 쌓아두면 다음부터는 최근 3개월 활동 패턴으로 더 정교하게 추천해드릴 수 있어요.")
                        .isNewUserRecommend(true)
                        .isAlreadyJoined(false)
                        .isIneligible(false)
                        .build())
                .build();
    }

    private SavingsRecommendResponse buildLowConfidenceResponse(List<FinancialProduct> availableProducts) {
        FinancialProduct fallbackProduct = availableProducts.stream()
                .filter(product -> SavingsProductKind.from(product).orElse(null) == SavingsProductKind.GREEN_STEP_UP)
                .findFirst()
                .orElse(availableProducts.get(0));

        SavingsProductScorer.ScoredProduct fallback = new SavingsProductScorer.ScoredProduct(
                fallbackProduct,
                35,
                formatRateLabel(fallbackProduct),
                "최근 3개월 활동 데이터가 부족해 특정 카테고리형 적금보다 범용형 적금을 우선 추천해드려요.",
                "최근 활동을 다시 쌓기 시작하면 E/S/G 패턴에 맞는 적금으로 더 정확하게 추천할 수 있어요.",
                false
        );

        return SavingsRecommendResponse.builder()
                .isNewUser(false)
                .recommendation(toResponse(fallback, false))
                .build();
    }

    private SavingsRecommendResponse buildNoAvailableResponse() {
        return SavingsRecommendResponse.builder()
                .isNewUser(false)
                .recommendation(null)
                .build();
    }

    private SavingsProductScorer.ScoredProduct buildGenericFallback(FinancialProduct product) {
        return new SavingsProductScorer.ScoredProduct(
                product,
                0,
                formatRateLabel(product),
                "현재 맞춤 규칙과 정확히 일치하는 추천 상품이 없어 가입 가능한 적금 중 하나를 안내해드려요.",
                "상품 상세에서 우대금리 구조를 확인하고 원하는 혜택 형태를 선택해보세요.",
                false
        );
    }

    private SavingsRecommendResponse.RecommendItem toResponse(
            SavingsProductScorer.ScoredProduct product,
            boolean isNewUserRecommend
    ) {
        return SavingsRecommendResponse.RecommendItem.builder()
                .productId(product.getProduct().getFinProductId())
                .productName(product.getProduct().getName())
                .matchScore(product.getMatchScore())
                .expectedMaxRate(product.getExpectedMaxRate())
                .reason(product.getReason())
                .actionable(product.getActionable())
                .isNewUserRecommend(isNewUserRecommend)
                .isAlreadyJoined(false)
                .isIneligible(product.isIneligible())
                .build();
    }

    private String formatRateLabel(FinancialProduct product) {
        return "연 " + product.getMaxRate().stripTrailingZeros().toPlainString() + "%";
    }
}
