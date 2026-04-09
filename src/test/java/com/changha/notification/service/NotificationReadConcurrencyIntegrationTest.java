package com.changha.notification.service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.changha.notification.controller.NotificationFixtures;
import com.changha.notification.dto.CreateNotificationRequest;
import com.changha.notification.repository.NotificationRepository;
import com.changha.notification.testsupport.AbstractMySqlIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("알림 읽기 동시성 통합 테스트")
class NotificationReadConcurrencyIntegrationTest extends AbstractMySqlIntegrationTest {

    @Autowired
    private NotificationApplicationService notificationApplicationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @DisplayName("동시 요청 시에도 읽음 처리 시간(readAt)은 1번만 기록되어야 한다")
    @Test
    void concurrentReadShouldWriteReadAtOnlyOnce() throws Exception {
        CreateNotificationRequest request = NotificationFixtures.createImmediateEmailRequest();
        Long notificationId = notificationApplicationService.accept(request).notificationId();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<Integer> first = CompletableFuture.supplyAsync(
                    () -> notificationRepository.markReadIfUnread(notificationId, 1001L, mutableClock.now()),
                    executor
            );
            CompletableFuture<Integer> second = CompletableFuture.supplyAsync(
                    () -> notificationRepository.markReadIfUnread(notificationId, 1001L, mutableClock.now()),
                    executor
            );

            int totalUpdated = first.get() + second.get();

            assertThat(totalUpdated).isEqualTo(1);
            assertThat(notificationRepository.findById(notificationId).orElseThrow().getReadAt()).isNotNull();
        }
    }
}
