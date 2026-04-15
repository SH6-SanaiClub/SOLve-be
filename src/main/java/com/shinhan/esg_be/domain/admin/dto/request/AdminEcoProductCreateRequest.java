package com.shinhan.esg_be.domain.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AdminEcoProductCreateRequest {

    @NotBlank(message = "상품명은 필수입니다.")
    private String name;

    @NotBlank(message = "상점명은 필수입니다.")
    private String storeName;

    @NotBlank(message = "카테고리는 필수입니다.")
    private String category;

    @NotNull(message = "가격은 필수입니다.")
    @Positive(message = "가격은 0보다 커야 합니다.")
    private Long price;

    @NotBlank(message = "이미지 URL은 필수입니다.")
    private String imageUrl;

    @NotBlank(message = "설명은 필수입니다.")
    private String description;

    @NotNull(message = "재고는 필수입니다.")
    @PositiveOrZero(message = "재고는 0 이상이어야 합니다.")
    private Integer stock;

    @NotNull(message = "활성 상태는 필수입니다.")
    private Boolean isActive;
}
