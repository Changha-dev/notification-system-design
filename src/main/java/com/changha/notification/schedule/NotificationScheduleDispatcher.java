package com.changha.notification.schedule;

import java.time.Duration;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;

import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.changha.notification.domain.NotificationSchedule;
import com.changha.notification.domain.NotificationScheduleStatus;
import com.changha.notification.event.NotificationScheduleCreatedEvent;
import com.changha.notification.repository.NotificationScheduleRepository;
import com.changha.notification.service.NotificationApplicationService;

@Component
@RequiredArgsConstructor
public class NotificationScheduleDispatcher {

    private static final Duration LOCK_AT_MOST_FOR = Duration.ofSeconds(30);
    private static final Duration LOCK_AT_LEAST_FOR = Duration.ofSeconds(1);

    private final NotificationScheduleRepository notificationScheduleRepository;
    private final NotificationApplicationService notificationApplicationService;
    private final TaskScheduler taskScheduler;
    private final LockProvider lockProvider;
    private final Clock clock;
    private final ConcurrentMap<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onScheduleCreated(NotificationScheduleCreatedEvent event) {
        register(event.scheduleId(), event.scheduleAt());
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializePendingSchedules() {
        LocalDateTime now = LocalDateTime.now(clock);
        dispatchDueSchedules(now);
        notificationScheduleRepository.findByStatusOrderByScheduledAtAsc(NotificationScheduleStatus.PENDING).stream()
                .filter(schedule -> schedule.getScheduledAt().isAfter(now))
                .forEach(schedule -> register(schedule.getId(), schedule.getScheduledAt()));
    }

    public void register(Long scheduleId, LocalDateTime scheduleAt) {
        Instant scheduledInstant = scheduleAt.atZone(clock.getZone()).toInstant();
        Instant executionInstant = scheduledInstant.isAfter(clock.instant()) ? scheduledInstant : clock.instant();

        scheduledTasks.compute(scheduleId, (id, existing) -> {
            if (existing != null && !existing.isDone()) {
                return existing;
            }
            return taskScheduler.schedule(() -> executeScheduledDispatch(id), executionInstant);
        });
    }

    public int dispatchDueSchedules() {
        return dispatchDueSchedules(LocalDateTime.now(clock));
    }

    private int dispatchDueSchedules(LocalDateTime now) {
        List<NotificationSchedule> schedules = notificationScheduleRepository.findDispatchableSchedules(
                NotificationScheduleStatus.PENDING,
                now
        );
        int dispatchedCount = 0;
        for (NotificationSchedule schedule : schedules) {
            if (tryDispatchScheduledNotification(schedule.getId())) {
                dispatchedCount += 1;
            }
        }
        return dispatchedCount;
    }

    private void executeScheduledDispatch(Long scheduleId) {
        scheduledTasks.remove(scheduleId);
        tryDispatchScheduledNotification(scheduleId);
    }

    private boolean tryDispatchScheduledNotification(Long scheduleId) {
        Optional<SimpleLock> lock = lockProvider.lock(new LockConfiguration(
                Instant.now(),
                scheduleLockName(scheduleId),
                LOCK_AT_MOST_FOR,
                LOCK_AT_LEAST_FOR
        ));
        if (lock.isEmpty()) {
            return false;
        }

        try {
            return notificationApplicationService.dispatchScheduledNotification(scheduleId);
        } finally {
            lock.get().unlock();
        }
    }

    private String scheduleLockName(Long scheduleId) {
        return "notificationScheduleDispatch:" + scheduleId;
    }
}
