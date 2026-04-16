package com.shinhan.esg_be.domain.admin.service;

import com.shinhan.esg_be.domain.admin.dto.request.AdminPenaltyRequest;
import com.shinhan.esg_be.domain.admin.dto.request.AdminUserStatusUpdateRequest;
import com.shinhan.esg_be.domain.admin.dto.response.AdminUserDetailResponse;
import com.shinhan.esg_be.domain.admin.dto.response.AdminUserListResponse;
import com.shinhan.esg_be.domain.admin.dto.response.AdminPageResponse;
import com.shinhan.esg_be.domain.score.service.ScorePenaltyService;
import com.shinhan.esg_be.domain.user.entity.User;
import com.shinhan.esg_be.domain.user.repository.UserRepository;
import com.shinhan.esg_be.global.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");
    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "userId",
            "loginId",
            "name",
            "email",
            "currentGrade",
            "userType",
            "isActive",
            "createdAt"
    );

    private final UserRepository userRepository;
    private final ScorePenaltyService scorePenaltyService;
    private final Clock clock;

    public AdminPageResponse<AdminUserListResponse> getUsers(String keyword, Pageable pageable) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        Pageable sanitizedPageable = sanitizePageable(pageable);
        Page<AdminUserListResponse> page = userRepository.searchAdminUsers(normalizedKeyword, sanitizedPageable)
                .map(AdminUserListResponse::from);
        return AdminPageResponse.from(page);
    }

    public AdminUserDetailResponse getUser(Long userId) {
        return AdminUserDetailResponse.from(getUserEntity(userId));
    }

    @Transactional
    public AdminUserDetailResponse updateUserStatus(Long userId, AdminUserStatusUpdateRequest request) {
        User user = getUserEntity(userId);
        user.updateIsActive(request.getIsActive());
        return AdminUserDetailResponse.from(user);
    }

    @Transactional
    public AdminUserDetailResponse applyPenalty(Long userId, AdminPenaltyRequest request) {
        getUserEntity(userId);
        log.info("Admin applied abuse penalty. userId={}, reason={}", userId, request.getReason());
        scorePenaltyService.applyAbusePenalty(userId, LocalDateTime.now(clock));
        return AdminUserDetailResponse.from(getUserEntity(userId));
    }

    private User getUserEntity(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("사용자를 찾을 수 없습니다."));
    }

    private Pageable sanitizePageable(Pageable pageable) {
        int pageNumber = pageable == null ? 0 : Math.max(pageable.getPageNumber(), 0);
        int requestedSize = pageable == null ? DEFAULT_PAGE_SIZE : pageable.getPageSize();
        int pageSize = requestedSize <= 0 ? DEFAULT_PAGE_SIZE : Math.min(requestedSize, MAX_PAGE_SIZE);

        if (pageable == null || pageable.getSort().isUnsorted()) {
            return PageRequest.of(pageNumber, pageSize, DEFAULT_SORT);
        }

        List<Sort.Order> validOrders = pageable.getSort()
                .stream()
                .filter(order -> ALLOWED_SORT_PROPERTIES.contains(order.getProperty()))
                .map(order -> order.isDescending()
                        ? Sort.Order.desc(order.getProperty())
                        : Sort.Order.asc(order.getProperty()))
                .toList();

        Sort sort = validOrders.isEmpty() ? DEFAULT_SORT : Sort.by(validOrders);
        return PageRequest.of(pageNumber, pageSize, sort);
    }
}
