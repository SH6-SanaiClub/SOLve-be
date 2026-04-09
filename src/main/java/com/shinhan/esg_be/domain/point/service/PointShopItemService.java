package com.shinhan.esg_be.domain.point.service;

import com.shinhan.esg_be.domain.point.dto.response.PointShopItemResponse;
import com.shinhan.esg_be.domain.point.dto.response.PointShopItemsResponse;
import com.shinhan.esg_be.domain.point.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointShopItemService {

    private final ItemRepository itemRepository;

    public PointShopItemsResponse getItems() {
        return new PointShopItemsResponse(itemRepository.findActiveItems());
    }

    public PointShopItemResponse getItem(Long itemId) {
        return itemRepository.findActiveItem(itemId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "상품을 찾을 수 없습니다."));
    }
}
