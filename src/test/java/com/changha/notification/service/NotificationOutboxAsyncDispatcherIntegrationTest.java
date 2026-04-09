package com.changha.notification.service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.changha.notification.controller.NotificationFixtures;
import com.changha.notification.domain.Notification;
import com.changha.notification.domain.NotificationOutbox;
import com.changha.notification.domain.NotificationOutboxStatus;
import com.changha.notification.domain.NotificationStatus;
import com.changha.notification.event.NotificationOutboxCreatedEvent;
import com.changha.notification.repository.NotificationOutboxRepository;
import com.changha.notification.repository.NotificationRepository;
import com.changha.notification.testsupport.AbstractMySqlIntegrationTest;
import com.changha.notification.util.NotificationDispatchExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@DisplayName("아웃박스 자동 비동기 디스패치 통합 테스트")
@TestPropertySource(properties = "app.notification.auto-dispatch-enabled=true")
class NotificationOutboxAsyncDispatcherIntegrationTest extends AbstractMySqlIntegrationTest {

    @Autowired
    private NotificationApplicationService notificationApplicationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationOutboxRepository notificationOutboxRepository;

    @Autowired
    private RecordingNotificationDispatchExecutor recordingDispatchExecutor;

    @Autowired
    private RollbackPublishingService rollbackPublishingService;

    @BeforeEach
    void resetDispatchExecutor() {
        recordingDispatchExecutor.reset();
    }

    @DisplayName("자동 디스패치가 켜져 있으면 커밋 이후 아웃박스가 자동으로 발송되어야 한다")
    @Test
    void shouldDispatchOutboxAutomaticallyAfterCommit() {
        Long notificationId = notificationApplicationService.accept(NotificationFixtures.createImmediateEmailRequest())
                .notificationId();
        Long outboxId = notificationOutboxRepository.findByNotificationId(notificationId).orElseThrow().getId();

        verify(emailGateway, timeout(5000).times(1)).send(any(), anyString());
        waitUntil(() -> {
            Notification notification = notificationRepository.findById(notificationId).orElseThrow();
            NotificationOutbox outbox = notificationOutboxRepository.findById(outboxId).orElseThrow();
            return notification.getStatus() == NotificationStatus.SENT
                    && notification.getSentAt() != null
                    && outbox.getStatus() == NotificationOutboxStatus.COMPLETED;
        });

        assertThat(recordingDispatchExecutor.executionCount()).isEqualTo(1);
    }

    @DisplayName("트랜잭션이 롤백되면 AFTER_COMMIT 리스너가 실행되지 않아야 한다")
    @Test
    void shouldNotDispatchWhenTransactionRollsBack() throws Exception {
        assertThatThrownBy(() -> rollbackPublishingService.publishAndRollback(9999L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("rollback");

        Thread.sleep(300);

        assertThat(recordingDispatchExecutor.executionCount()).isZero();
        verify(emailGateway, never()).send(any(), anyString());
    }

    private void waitUntil(Check check) {
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            if (check.isSatisfied()) {
                return;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(exception);
            }
        }
        throw new AssertionError("Condition was not satisfied within timeout");
    }

    @FunctionalInterface
    interface Check {
        boolean isSatisfied();
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        RecordingNotificationDispatchExecutor recordingNotificationDispatchExecutor() {
            return new RecordingNotificationDispatchExecutor();
        }

        @Bean
        RollbackPublishingService rollbackPublishingService(ApplicationEventPublisher eventPublisher) {
            return new RollbackPublishingService(eventPublisher);
        }
    }

    static class RecordingNotificationDispatchExecutor extends NotificationDispatchExecutor {

        private final AtomicInteger executionCount = new AtomicInteger();

        RecordingNotificationDispatchExecutor() {
            super("recording-test", new SimpleAsyncTaskExecutor("notification-test-"));
        }

        @Override
        public CompletableFuture<Void> execute(Runnable task) {
            executionCount.incrementAndGet();
            return super.execute(task);
        }

        int executionCount() {
            return executionCount.get();
        }

        void reset() {
            executionCount.set(0);
        }
    }

    static class RollbackPublishingService {

        private final ApplicationEventPublisher eventPublisher;

        RollbackPublishingService(ApplicationEventPublisher eventPublisher) {
            this.eventPublisher = eventPublisher;
        }

        @Transactional
        public void publishAndRollback(Long outboxId) {
            eventPublisher.publishEvent(new NotificationOutboxCreatedEvent(outboxId));
            throw new IllegalStateException("force rollback");
        }
    }
}
