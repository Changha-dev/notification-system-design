package com.changha.notification.schedule;

import java.sql.Timestamp;
import java.time.Instant;

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

    @DisplayName("ShedLock이 이미 점유 중이면 스케줄 메서드 실행을 건너뛰어야 한다")
    @Test
    void scheduledMethodShouldSkipDispatchWhenShedLockIsAlreadyHeld() {
        CreateNotificationRequest request = NotificationFixtures.createScheduledEmailRequest(mutableClock.now().plusMinutes(1));
        Long scheduleId = notificationApplicationService.accept(request).scheduleId();
        Instant now = Instant.now();

        jdbcTemplate.update("""
                        insert into shedlock (name, lock_until, locked_at, locked_by)
                        values (?, ?, ?, ?)
                        on duplicate key update
                            lock_until = values(lock_until),
                            locked_at = values(locked_at),
                            locked_by = values(locked_by)
                        """,
                "scheduledNotificationDispatch",
                Timestamp.from(now.plusSeconds(30)),
                Timestamp.from(now),
                "test-lock"
        );

        mutableClock.advanceSeconds(61);

        notificationScheduleDispatcher.scheduledDispatchDueSchedules();

        assertThat(notificationRepository.count()).isZero();
        assertThat(notificationOutboxRepository.count()).isZero();
        assertThat(notificationScheduleRepository.findById(scheduleId).orElseThrow().getStatus().name()).isEqualTo("PENDING");

        jdbcTemplate.update("delete from shedlock where name = ?", "scheduledNotificationDispatch");

        int dispatched = notificationScheduleDispatcher.dispatchDueSchedules();

        assertThat(dispatched).isEqualTo(1);
        assertThat(notificationRepository.count()).isEqualTo(1);
        assertThat(notificationOutboxRepository.count()).isEqualTo(1);
        assertThat(notificationScheduleRepository.findById(scheduleId).orElseThrow().getStatus().name()).isEqualTo("DISPATCHED");
    }
}
