package com.changha.notification.dto;

import java.util.List;

public record NotificationListResponse(
        List<NotificationSummaryResponse> content,
        int page,
        int size,
        boolean hasNext
) {
}
