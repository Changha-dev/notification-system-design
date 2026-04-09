package com.changha.notification.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.changha.notification.domain.NotificationChannel;
import com.changha.notification.domain.NotificationSchedule;
import com.changha.notification.domain.NotificationScheduleStatus;
import com.changha.notification.domain.NotificationType;

public interface NotificationScheduleRepository extends JpaRepository<NotificationSchedule, Long> {

    java.util.Optional<NotificationSchedule> findByRecipientIdAndNotificationTypeAndReferenceIdAndChannelAndScheduledAt(
            Long recipientId,
            NotificationType notificationType,
            Long referenceId,
            NotificationChannel channel,
            LocalDateTime scheduledAt
    );

    @Query("""
        select schedule
          from NotificationSchedule schedule
         where schedule.status = :status
           and schedule.scheduledAt <= :scheduledAt
         order by schedule.scheduledAt, schedule.id
    """)
    List<NotificationSchedule> findDispatchableSchedules(
            @Param("status") NotificationScheduleStatus status,
            @Param("scheduledAt") LocalDateTime scheduledAt
    );
}
