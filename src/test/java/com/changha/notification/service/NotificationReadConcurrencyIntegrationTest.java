package com.changha.notification.service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.changha.notification.controller.NotificationFixtures;
import com.changha.notification.dto.CreateNotificationRequest;
import com.changha.notification.repository.NotificationRepository;
import com.changha.notification.testsupport.AbstractMySqlIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationReadConcurrencyIntegrationTest extends AbstractMySqlIntegrationTest {

    @Autowired
    private NotificationApplicationService notificationApplicationService;

    @Autowired
    private NotificationRepository notificationRepository;

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
