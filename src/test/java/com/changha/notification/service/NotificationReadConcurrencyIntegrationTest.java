package com.changha.notification.service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.changha.notification.controller.NotificationFixtures;
import com.changha.notification.dto.CreateNotificationRequest;
import com.changha.notification.dto.ReadNotificationResponse;
import com.changha.notification.repository.MemberStatsRepository;
import com.changha.notification.repository.NotificationRepository;
import com.changha.notification.testsupport.AbstractMySqlIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("알림 읽기 동시성 통합 테스트")
class NotificationReadConcurrencyIntegrationTest extends AbstractMySqlIntegrationTest {

    @Autowired
    private NotificationApplicationService notificationApplicationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private MemberStatsRepository memberStatsRepository;

    @DisplayName("동시 요청 시에도 읽음 처리 시간(readAt)은 1번만 기록되어야 한다")
    @Test
    void concurrentReadShouldWriteReadAtOnlyOnce() throws Exception {
        CreateNotificationRequest request = NotificationFixtures.createImmediateEmailRequest();
        Long notificationId = notificationApplicationService.accept(request).notificationId();
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<ReadNotificationResponse> first = CompletableFuture.supplyAsync(
                    () -> {
                        await(start);
                        return notificationApplicationService.markRead(notificationId, 1001L);
                    },
                    executor
            );
            CompletableFuture<ReadNotificationResponse> second = CompletableFuture.supplyAsync(
                    () -> {
                        await(start);
                        return notificationApplicationService.markRead(notificationId, 1001L);
                    },
                    executor
            );

            start.countDown();

            ReadNotificationResponse firstResponse = first.get();
            ReadNotificationResponse secondResponse = second.get();
            var notification = notificationRepository.findById(notificationId).orElseThrow();

            assertThat(firstResponse.isRead()).isTrue();
            assertThat(secondResponse.isRead()).isTrue();
            assertThat(firstResponse.readAt()).isNotNull();
            assertThat(secondResponse.readAt()).isNotNull();
            assertThat(firstResponse.readAt()).isEqualTo(secondResponse.readAt());
            assertThat(notification.getReadAt()).isEqualTo(firstResponse.readAt());
            assertThat(notificationRepository.countByRecipientIdAndReadAtIsNull(1001L)).isZero();
            assertThat(memberStatsRepository.findById(1001L).orElseThrow().getUnreadCount()).isZero();
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }
}
