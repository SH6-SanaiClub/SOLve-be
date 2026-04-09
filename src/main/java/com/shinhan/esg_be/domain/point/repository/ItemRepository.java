package com.shinhan.esg_be.domain.point.repository;

import com.shinhan.esg_be.domain.point.dto.response.PointShopItemResponse;
import com.shinhan.esg_be.domain.point.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long> {

    @Query("""
            select new com.shinhan.esg_be.domain.point.dto.response.PointShopItemResponse(
                i.itemId,
                i.name,
                i.category,
                i.requiredPoints,
                i.imageUrl,
                i.description,
                i.stock
            )
            from Item i
            where i.isActive = true
            order by i.itemId desc
            """)
    List<PointShopItemResponse> findActiveItems();

    @Query("""
            select new com.shinhan.esg_be.domain.point.dto.response.PointShopItemResponse(
                i.itemId,
                i.name,
                i.category,
                i.requiredPoints,
                i.imageUrl,
                i.description,
                i.stock
            )
            from Item i
            where i.itemId = :itemId
              and i.isActive = true
            """)
    Optional<PointShopItemResponse> findActiveItem(@Param("itemId") Long itemId);
}
