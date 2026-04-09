package com.changha.notification.schedule;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.changha.notification.domain.NotificationSchedule;
import com.changha.notification.domain.NotificationScheduleStatus;
import com.changha.notification.repository.NotificationScheduleRepository;
import com.changha.notification.service.NotificationApplicationService;

@Component
@RequiredArgsConstructor
public class NotificationScheduleDispatcher {

    private final NotificationScheduleRepository notificationScheduleRepository;
    private final NotificationApplicationService notificationApplicationService;
    private final Clock clock;

    @Scheduled(fixedDelayString = "5000")
    @SchedulerLock(name = "scheduledNotificationDispatch", lockAtMostFor = "PT30S", lockAtLeastFor = "PT1S")
    public void scheduledDispatchDueSchedules() {
        dispatchDueSchedules();
    }

    public int dispatchDueSchedules() {
        List<NotificationSchedule> schedules = notificationScheduleRepository.findDispatchableSchedules(
                NotificationScheduleStatus.PENDING,
                LocalDateTime.now(clock)
        );
        int dispatchedCount = 0;
        for (NotificationSchedule schedule : schedules) {
            if (notificationApplicationService.dispatchScheduledNotification(schedule.getId())) {
                dispatchedCount += 1;
            }
        }
        return dispatchedCount;
    }
}
