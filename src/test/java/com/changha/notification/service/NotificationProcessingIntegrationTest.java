package com.changha.notification.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.changha.notification.controller.NotificationFixtures;
import com.changha.notification.domain.Notification;
import com.changha.notification.domain.NotificationOutbox;
import com.changha.notification.domain.NotificationOutboxStatus;
import com.changha.notification.domain.NotificationStatus;
import com.changha.notification.dto.CreateNotificationRequest;
import com.changha.notification.repository.NotificationOutboxRepository;
import com.changha.notification.repository.NotificationRepository;
import com.changha.notification.schedule.NotificationRecoveryScheduler;
import com.changha.notification.sender.PermanentNotificationDispatchException;
import com.changha.notification.sender.RetryableNotificationDispatchException;
import com.changha.notification.testsupport.AbstractMySqlIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class NotificationProcessingIntegrationTest extends AbstractMySqlIntegrationTest {

    @Autowired
    private NotificationApplicationService notificationApplicationService;

    @Autowired
    private NotificationOutboxProcessor notificationOutboxProcessor;

    @Autowired
    private NotificationOutboxRepository notificationOutboxRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationRecoveryScheduler notificationRecoveryScheduler;

    @Test
    void processPendingOutboxShouldMarkSuccess() {
        Long outboxId = createImmediateAndGetOutboxId(NotificationFixtures.createImmediateEmailRequest());

        boolean claimed = notificationOutboxProcessor.processPendingOutbox(outboxId, "worker-1");

        Notification notification = notificationRepository.findAll().getFirst();
        NotificationOutbox outbox = notificationOutboxRepository.findById(outboxId).orElseThrow();

        assertThat(claimed).isTrue();
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(notification.getSentAt()).isNotNull();
        assertThat(outbox.getStatus()).isEqualTo(NotificationOutboxStatus.COMPLETED);
        verify(emailGateway, times(1)).send(any(), anyString());
    }

    @Test
    void retryableFailureShouldIncreaseRetryCountAndEventuallySucceed() {
        Long outboxId = createImmediateAndGetOutboxId(NotificationFixtures.createImmediateEmailRequest());
        doThrow(new RetryableNotificationDispatchException("RATE_LIMIT", "Too many requests"))
                .doNothing()
                .when(emailGateway)
                .send(any(), anyString());

        notificationOutboxProcessor.processPendingOutbox(outboxId, "worker-1");

        NotificationOutbox firstAttempt = notificationOutboxRepository.findById(outboxId).orElseThrow();
        Notification firstNotification = notificationRepository.findAll().getFirst();
        assertThat(firstAttempt.getStatus()).isEqualTo(NotificationOutboxStatus.PENDING);
        assertThat(firstAttempt.getRetryCount()).isEqualTo(1);
        assertThat(firstNotification.getStatus()).isEqualTo(NotificationStatus.PENDING);

        mutableClock.advanceSeconds(2);
        notificationOutboxProcessor.processPendingOutbox(outboxId, "worker-2");

        NotificationOutbox secondAttempt = notificationOutboxRepository.findById(outboxId).orElseThrow();
        Notification secondNotification = notificationRepository.findAll().getFirst();
        assertThat(secondAttempt.getStatus()).isEqualTo(NotificationOutboxStatus.COMPLETED);
        assertThat(secondAttempt.getRetryCount()).isEqualTo(1);
        assertThat(secondNotification.getStatus()).isEqualTo(NotificationStatus.SENT);
        verify(emailGateway, times(2)).send(any(), anyString());
    }

    @Test
    void repeatedRetryableFailureShouldBecomeDead() {
        Long outboxId = createImmediateAndGetOutboxId(NotificationFixtures.createImmediateEmailRequest());
        doThrow(new RetryableNotificationDispatchException("NETWORK_TIMEOUT", "Timed out"))
                .when(emailGateway)
                .send(any(), anyString());

        notificationOutboxProcessor.processPendingOutbox(outboxId, "worker-1");
        mutableClock.advanceSeconds(2);
        notificationOutboxProcessor.processPendingOutbox(outboxId, "worker-2");
        mutableClock.advanceSeconds(4);
        notificationOutboxProcessor.processPendingOutbox(outboxId, "worker-3");

        NotificationOutbox outbox = notificationOutboxRepository.findById(outboxId).orElseThrow();
        Notification notification = notificationRepository.findAll().getFirst();
        assertThat(outbox.getStatus()).isEqualTo(NotificationOutboxStatus.DEAD);
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.FAILED);
        verify(emailGateway, times(3)).send(any(), anyString());
    }

    @Test
    void recoverySchedulerShouldRequeueStaleProcessingAndDispatchAgain() {
        Long outboxId = createImmediateAndGetOutboxId(NotificationFixtures.createImmediateEmailRequest());

        boolean claimed = notificationOutboxRepository.claimPending(outboxId, "stuck-worker", mutableClock.now());
        assertThat(claimed).isTrue();

        mutableClock.advanceSeconds(10);
        int processed = notificationRecoveryScheduler.recoverAndDispatch();

        Notification notification = notificationRepository.findAll().getFirst();
        NotificationOutbox outbox = notificationOutboxRepository.findById(outboxId).orElseThrow();
        assertThat(processed).isEqualTo(1);
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(outbox.getStatus()).isEqualTo(NotificationOutboxStatus.COMPLETED);
        verify(emailGateway, times(1)).send(any(), anyString());
    }

    @Test
    void concurrentWorkersShouldAllowOnlySingleClaim() throws Exception {
        Long outboxId = createImmediateAndGetOutboxId(NotificationFixtures.createImmediateEmailRequest());
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<Boolean> first = CompletableFuture.supplyAsync(() -> {
                await(start);
                return notificationOutboxProcessor.processPendingOutbox(outboxId, "worker-a");
            }, executor);

            CompletableFuture<Boolean> second = CompletableFuture.supplyAsync(() -> {
                await(start);
                return notificationOutboxProcessor.processPendingOutbox(outboxId, "worker-b");
            }, executor);

            start.countDown();

            List<Boolean> results = List.of(first.get(), second.get());
            assertThat(results).containsExactlyInAnyOrder(true, false);
            verify(emailGateway, times(1)).send(any(), anyString());
        }
    }

    @Test
    void permanentFailureShouldBecomeDeadImmediately() {
        Long outboxId = createImmediateAndGetOutboxId(NotificationFixtures.createImmediateEmailRequest());
        doThrow(new PermanentNotificationDispatchException("INVALID_EMAIL", "Invalid destination"))
                .when(emailGateway)
                .send(any(), anyString());

        notificationOutboxProcessor.processPendingOutbox(outboxId, "worker-1");

        NotificationOutbox outbox = notificationOutboxRepository.findById(outboxId).orElseThrow();
        Notification notification = notificationRepository.findAll().getFirst();
        assertThat(outbox.getStatus()).isEqualTo(NotificationOutboxStatus.DEAD);
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.FAILED);
    }

    private Long createImmediateAndGetOutboxId(CreateNotificationRequest request) {
        Long notificationId = notificationApplicationService.accept(request).notificationId();
        return notificationOutboxRepository.findByNotificationId(notificationId).orElseThrow().getId();
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
