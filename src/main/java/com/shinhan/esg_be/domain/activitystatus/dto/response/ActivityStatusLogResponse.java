package com.shinhan.esg_be.domain.activitystatus.dto.response;

import java.util.List;

public record ActivityStatusLogResponse(
        Integer totalCount,
        Boolean hasNext,
        String nextCursor,
        List<ActivityStatusLogItemResponse> items
) {
}
