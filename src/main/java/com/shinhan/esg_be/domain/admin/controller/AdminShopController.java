package com.shinhan.esg_be.domain.admin.controller;

import com.shinhan.esg_be.domain.admin.dto.request.AdminItemCreateRequest;
import com.shinhan.esg_be.domain.admin.dto.request.AdminItemUpdateRequest;
import com.shinhan.esg_be.domain.admin.dto.request.AdminStatusUpdateRequest;
import com.shinhan.esg_be.domain.admin.dto.response.AdminItemResponse;
import com.shinhan.esg_be.domain.admin.service.AdminShopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/items")
@RequiredArgsConstructor
public class AdminShopController {

    private final AdminShopService adminShopService;

    @GetMapping
    public ResponseEntity<List<AdminItemResponse>> getItems() {
        return ResponseEntity.ok(adminShopService.getItems());
    }

    @PostMapping
    public ResponseEntity<AdminItemResponse> createItem(@RequestBody @Valid AdminItemCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminShopService.createItem(request));
    }

    @PutMapping("/{itemId}")
    public ResponseEntity<AdminItemResponse> updateItem(
            @PathVariable Long itemId,
            @RequestBody @Valid AdminItemUpdateRequest request
    ) {
        return ResponseEntity.ok(adminShopService.updateItem(itemId, request));
    }

    @PatchMapping("/{itemId}/status")
    public ResponseEntity<AdminItemResponse> updateItemStatus(
            @PathVariable Long itemId,
            @RequestBody @Valid AdminStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(adminShopService.updateItemStatus(itemId, request));
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long itemId) {
        adminShopService.deleteItem(itemId);
        return ResponseEntity.noContent().build();
    }
}
