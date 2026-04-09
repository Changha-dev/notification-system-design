package com.changha.notification.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import com.changha.notification.domain.NotificationChannel;
import com.changha.notification.domain.NotificationSchedule;
import com.changha.notification.domain.NotificationScheduleStatus;
import com.changha.notification.domain.NotificationType;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import jakarta.persistence.QueryHint;

public interface NotificationScheduleRepository extends JpaRepository<NotificationSchedule, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")})
    @Query("select s from NotificationSchedule s where s.id = :id")
    java.util.Optional<NotificationSchedule> findByIdForUpdate(@Param("id") Long id);

    java.util.Optional<NotificationSchedule> findByRecipientIdAndNotificationTypeAndReferenceIdAndChannelAndScheduledAt(
            Long recipientId,
            NotificationType notificationType,
            Long referenceId,
            NotificationChannel channel,
            LocalDateTime scheduledAt
    );

    List<NotificationSchedule> findByStatusOrderByScheduledAtAsc(NotificationScheduleStatus status);

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
