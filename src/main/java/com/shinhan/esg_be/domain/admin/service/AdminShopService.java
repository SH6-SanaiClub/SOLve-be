package com.shinhan.esg_be.domain.admin.service;

import com.shinhan.esg_be.domain.admin.dto.request.AdminItemCreateRequest;
import com.shinhan.esg_be.domain.admin.dto.request.AdminItemUpdateRequest;
import com.shinhan.esg_be.domain.admin.dto.request.AdminStatusUpdateRequest;
import com.shinhan.esg_be.domain.admin.dto.response.AdminItemResponse;
import com.shinhan.esg_be.domain.point.entity.Item;
import com.shinhan.esg_be.domain.point.repository.ItemRepository;
import com.shinhan.esg_be.domain.point.repository.UserPointRepository;
import com.shinhan.esg_be.global.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminShopService {

    private final ItemRepository itemRepository;
    private final UserPointRepository userPointRepository;

    public List<AdminItemResponse> getItems() {
        return itemRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(item -> AdminItemResponse.from(
                        item,
                        userPointRepository.countByItem_ItemId(item.getItemId())
                ))
                .toList();
    }

    @Transactional
    public AdminItemResponse createItem(AdminItemCreateRequest request) {
        Item item = itemRepository.save(
                Item.create(
                        request.getName(),
                        request.getCategory(),
                        request.getRequiredPoints(),
                        request.getImageUrl(),
                        request.getDescription(),
                        request.getStock(),
                        request.getIsActive()
                )
        );
        return AdminItemResponse.from(item, 0L);
    }

    @Transactional
    public AdminItemResponse updateItem(Long itemId, AdminItemUpdateRequest request) {
        Item item = getItemEntity(itemId);
        item.update(
                request.getName(),
                request.getCategory(),
                request.getRequiredPoints(),
                request.getImageUrl(),
                request.getDescription(),
                request.getStock()
        );
        return AdminItemResponse.from(item, userPointRepository.countByItem_ItemId(itemId));
    }

    @Transactional
    public AdminItemResponse updateItemStatus(Long itemId, AdminStatusUpdateRequest request) {
        Item item = getItemEntity(itemId);
        item.updateStatus(request.getIsActive());
        return AdminItemResponse.from(item, userPointRepository.countByItem_ItemId(itemId));
    }

    @Transactional
    public void deleteItem(Long itemId) {
        Item item = getItemEntity(itemId);
        if (userPointRepository.countByItem_ItemId(itemId) > 0) {
            throw new BadRequestException("교환 이력이 있는 포인트샵 상품은 삭제할 수 없습니다.");
        }
        itemRepository.delete(item);
    }

    private Item getItemEntity(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new BadRequestException("포인트샵 상품을 찾을 수 없습니다."));
    }
}
