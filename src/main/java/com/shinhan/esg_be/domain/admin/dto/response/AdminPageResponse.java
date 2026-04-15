package com.shinhan.esg_be.domain.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@AllArgsConstructor
public class AdminPageResponse<T> {

    private List<T> content;
    private int page;
    private int size;
    private int totalPages;
    private long totalElements;
    private int numberOfElements;
    private boolean first;
    private boolean last;
    private boolean hasNext;
    private boolean hasPrevious;

    public static <T> AdminPageResponse<T> from(Page<T> page) {
        return new AdminPageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.getNumberOfElements(),
                page.isFirst(),
                page.isLast(),
                page.hasNext(),
                page.hasPrevious()
        );
    }
}
