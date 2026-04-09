package com.changha.notification.schedule;

import java.sql.Timestamp;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.changha.notification.controller.NotificationFixtures;
import com.changha.notification.domain.NotificationChannel;
import com.changha.notification.domain.NotificationSchedule;
import com.changha.notification.domain.NotificationType;
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

    @DisplayName("예약 요청이 등록되면 TaskScheduler가 예약 시각에 맞춰 알림과 아웃박스를 생성해야 한다")
    @Test
    void taskSchedulerShouldCreateNotificationAndOutboxWhenTimeArrives() {
        CreateNotificationRequest request = NotificationFixtures.createScheduledEmailRequest(mutableClock.now().plusNanos(300_000_000));
        Long scheduleId = notificationApplicationService.accept(request).scheduleId();

        assertThat(notificationRepository.count()).isZero();

        waitUntil(() -> notificationRepository.count() == 1 && notificationOutboxRepository.count() == 1);

        assertThat(notificationScheduleRepository.findById(scheduleId).orElseThrow().getStatus().name()).isEqualTo("DISPATCHED");
    }

    @DisplayName("애플리케이션 시작 시 이미 기한이 지난 예약 건은 즉시 복구되어 발송되어야 한다")
    @Test
    void initializePendingSchedulesShouldDispatchOverdueSchedules() {
        NotificationSchedule schedule = notificationScheduleRepository.save(new NotificationSchedule(
                1001L,
                NotificationType.COURSE_START_D_MINUS_1,
                7001L,
                NotificationChannel.EMAIL,
                mutableClock.now().minusMinutes(1)
        ));

        int beforeDispatch = notificationRepository.findAll().size();

        notificationScheduleDispatcher.initializePendingSchedules();

        assertThat(notificationRepository.findAll()).hasSize(beforeDispatch + 1);
        assertThat(notificationOutboxRepository.count()).isEqualTo(1);
        assertThat(notificationScheduleRepository.findById(schedule.getId()).orElseThrow().getStatus().name()).isEqualTo("DISPATCHED");
    }

    @DisplayName("동일한 예약 건에 대한 분산락이 이미 점유 중이면 실행을 건너뛰어야 한다")
    @Test
    void dispatchDueSchedulesShouldSkipWhenScheduleLockIsAlreadyHeld() {
        NotificationSchedule schedule = notificationScheduleRepository.save(new NotificationSchedule(
                1001L,
                NotificationType.COURSE_START_D_MINUS_1,
                7001L,
                NotificationChannel.EMAIL,
                mutableClock.now().minusMinutes(1)
        ));
        Instant now = Instant.now();

        jdbcTemplate.update("""
                        insert into shedlock (name, lock_until, locked_at, locked_by)
                        values (?, ?, ?, ?)
                        on duplicate key update
                            lock_until = values(lock_until),
                            locked_at = values(locked_at),
                            locked_by = values(locked_by)
                        """,
                "notificationScheduleDispatch:" + schedule.getId(),
                Timestamp.from(now.plusSeconds(30)),
                Timestamp.from(now),
                "test-lock"
        );

        int skipped = notificationScheduleDispatcher.dispatchDueSchedules();

        assertThat(skipped).isZero();
        assertThat(notificationRepository.count()).isZero();
        assertThat(notificationOutboxRepository.count()).isZero();
        assertThat(notificationScheduleRepository.findById(schedule.getId()).orElseThrow().getStatus().name()).isEqualTo("PENDING");

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
}
