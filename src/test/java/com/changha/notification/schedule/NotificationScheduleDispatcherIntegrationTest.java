package com.changha.notification.schedule;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.changha.notification.controller.NotificationFixtures;
import com.changha.notification.dto.CreateNotificationRequest;
import com.changha.notification.repository.NotificationOutboxRepository;
import com.changha.notification.repository.NotificationRepository;
import com.changha.notification.repository.NotificationScheduleRepository;
import com.changha.notification.service.NotificationApplicationService;
import com.changha.notification.testsupport.AbstractMySqlIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationScheduleDispatcherIntegrationTest extends AbstractMySqlIntegrationTest {

    @Autowired
    private NotificationApplicationService notificationApplicationService;

    @Autowired
    private NotificationScheduleDispatcher notificationScheduleDispatcher;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationOutboxRepository notificationOutboxRepository;

    @Autowired
    private NotificationScheduleRepository notificationScheduleRepository;

    @Test
    void scheduleDispatcherShouldCreateNotificationAndOutboxWhenTimeArrives() {
        CreateNotificationRequest request = NotificationFixtures.createScheduledEmailRequest(mutableClock.now().plusMinutes(10));
        Long scheduleId = notificationApplicationService.accept(request).scheduleId();

        assertThat(notificationRepository.count()).isZero();
        mutableClock.advanceSeconds(601);

        int dispatched = notificationScheduleDispatcher.dispatchDueSchedules();

        assertThat(dispatched).isEqualTo(1);
        assertThat(notificationScheduleRepository.findById(scheduleId).orElseThrow().getStatus().name()).isEqualTo("DISPATCHED");
        assertThat(notificationRepository.count()).isEqualTo(1);
        assertThat(notificationOutboxRepository.count()).isEqualTo(1);
    }

    @Test
    void shedLockShouldPreventDuplicateDispatchUnderConcurrentCalls() throws Exception {
        CreateNotificationRequest request = NotificationFixtures.createScheduledEmailRequest(mutableClock.now().plusMinutes(1));
        notificationApplicationService.accept(request);
        mutableClock.advanceSeconds(61);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<Integer> first = CompletableFuture.supplyAsync(
                    notificationScheduleDispatcher::dispatchDueSchedules,
                    executor
            );
            CompletableFuture<Integer> second = CompletableFuture.supplyAsync(
                    notificationScheduleDispatcher::dispatchDueSchedules,
                    executor
            );

            List<Integer> results = List.of(first.get(), second.get());

            assertThat(results.stream().mapToInt(Integer::intValue).sum()).isEqualTo(1);
            assertThat(notificationRepository.count()).isEqualTo(1);
            assertThat(notificationOutboxRepository.count()).isEqualTo(1);
        }
    }
}
