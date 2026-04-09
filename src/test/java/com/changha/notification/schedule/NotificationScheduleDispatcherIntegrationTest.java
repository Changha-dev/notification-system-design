package com.changha.notification.schedule;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.DisplayName;
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

@DisplayName("예약 알림 디스패처 통합 테스트")
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

    @DisplayName("예약 시간에 도달하면 알림과 아웃박스가 생성되어야 한다")
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

    @DisplayName("다수의 인스턴스 또는 스레드에서 동시 처리 시 잠금을 통해 중복 발송을 방지해야 한다")
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
