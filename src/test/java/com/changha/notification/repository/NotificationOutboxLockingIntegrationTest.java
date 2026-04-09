package com.changha.notification.repository;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.changha.notification.controller.NotificationFixtures;
import com.changha.notification.service.NotificationApplicationService;
import com.changha.notification.testsupport.AbstractMySqlIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("아웃박스 잠금(Locking) 통합 테스트")
class NotificationOutboxLockingIntegrationTest extends AbstractMySqlIntegrationTest {

    @Autowired
    private NotificationApplicationService notificationApplicationService;

    @Autowired
    private NotificationOutboxRepository notificationOutboxRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @DisplayName("아웃박스 배치 폴링 시 락이 걸린 행(Row)은 건너뛰고 나머지 행만 가져와야 한다")
    @Test
    void claimPendingBatchShouldSkipLockedRows() throws Exception {
        Long firstOutboxId = createOutboxId(5001L);
        Long secondOutboxId = createOutboxId(5002L);

        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        Future<?> future = executorService.submit(() -> transactionTemplate.executeWithoutResult(status -> {
            jdbcTemplate.queryForObject("select id from notification_outbox where id = ? for update", Long.class, firstOutboxId);
            locked.countDown();
            try {
                release.await();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }));

        locked.await();
        List<Long> claimed = notificationOutboxRepository.claimPendingBatch("scheduler-1", mutableClock.now(), 10);

        assertThat(claimed).contains(secondOutboxId);
        assertThat(claimed).doesNotContain(firstOutboxId);

        release.countDown();
        future.get();
        executorService.shutdownNow();
    }

    private Long createOutboxId(Long referenceId) {
        Long notificationId = notificationApplicationService.accept(
                new com.changha.notification.dto.CreateNotificationRequest(
                        1001L,
                        com.changha.notification.domain.NotificationType.PAYMENT_CONFIRMED,
                        com.changha.notification.domain.NotificationChannel.EMAIL,
                        referenceId,
                        null
                )
        ).notificationId();
        return notificationOutboxRepository.findByNotificationId(notificationId).orElseThrow().getId();
    }
}
