package com.changha.notification.dto;

import java.util.List;

public record NotificationListResponse(
        List<NotificationSummaryResponse> content,
        long unreadCount,
        int page,
        int size,
        boolean hasNext
) {
}
