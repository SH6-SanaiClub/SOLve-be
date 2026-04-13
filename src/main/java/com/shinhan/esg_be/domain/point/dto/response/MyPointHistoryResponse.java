package com.shinhan.esg_be.domain.point.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class MyPointHistoryResponse {

    private final List<MyPointHistoryItemResponse> histories;
}
