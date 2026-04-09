package com.changha.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.changha.notification.dto.CreateNotificationRequest;
import com.changha.notification.dto.NotificationAcceptedResponse;
import com.changha.notification.dto.NotificationDetailResponse;
import com.changha.notification.dto.NotificationListResponse;
import com.changha.notification.dto.ReadFilter;
import com.changha.notification.dto.ReadNotificationResponse;
import com.changha.notification.service.NotificationApplicationService;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "알림 생성, 조회, 읽음 처리 API")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationApplicationService notificationApplicationService;

    @PostMapping
    @Operation(summary = "알림 발송 요청", description = "즉시 발송 또는 예약 발송 알림을 접수합니다.")
    public ResponseEntity<NotificationAcceptedResponse> createNotification(@Validated @RequestBody CreateNotificationRequest request) {
        return ResponseEntity.accepted().body(notificationApplicationService.accept(request));
    }

    @GetMapping("/{notificationId}")
    @Operation(summary = "단건 알림 조회", description = "사용자 기준으로 특정 알림 상세 정보를 조회합니다.")
    public NotificationDetailResponse getNotification(
            @Parameter(description = "조회할 알림 ID", required = true)
            @PathVariable Long notificationId,
            @Parameter(description = "조회 사용자 ID", required = true, example = "1001")
            @RequestHeader("X-USER-ID") Long userId
    ) {
        return notificationApplicationService.getDetail(notificationId, userId);
    }

    @GetMapping
    @Operation(summary = "사용자 알림 목록 조회", description = "읽음/안읽음 필터와 페이지 조건으로 알림 목록을 조회합니다.")
    public NotificationListResponse getNotifications(
            @Parameter(description = "조회 사용자 ID", required = true, example = "1001")
            @RequestHeader("X-USER-ID") Long userId,
            @Parameter(description = "읽음 필터", example = "ALL")
            @RequestParam(defaultValue = "ALL") ReadFilter read,
            @Parameter(description = "페이지 번호", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        return notificationApplicationService.getList(userId, read, page, size);
    }

    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "알림 읽음 처리", description = "지정한 알림을 읽음 상태로 변경합니다.")
    public ReadNotificationResponse markRead(
            @Parameter(description = "읽음 처리할 알림 ID", required = true)
            @PathVariable Long notificationId,
            @Parameter(description = "읽음 처리 사용자 ID", required = true, example = "1001")
            @RequestHeader("X-USER-ID") Long userId
    ) {
        return notificationApplicationService.markRead(notificationId, userId);
    }
}
