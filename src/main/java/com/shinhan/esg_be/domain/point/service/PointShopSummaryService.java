package com.shinhan.esg_be.domain.point.service;

import com.shinhan.esg_be.domain.point.dto.response.PointShopSummaryResponse;
import com.shinhan.esg_be.domain.user.entity.User;
import com.shinhan.esg_be.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointShopSummaryService {

    private final UserRepository userRepository;

    public PointShopSummaryResponse getSummary(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "사용자 정보를 찾을 수 없습니다."));

        return new PointShopSummaryResponse(
                user.getUserId(),
                user.getTotalPoints().longValue()
        );
    }
}
