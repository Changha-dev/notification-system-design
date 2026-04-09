package com.changha.notification.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "notification_schedules",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_schedule_unique",
                        columnNames = {"recipient_id", "notification_type", "reference_id", "channel", "scheduled_at"}
                )
        },
        indexes = {
                @Index(name = "idx_schedule_status_time", columnList = "status, scheduled_at, id")
        }
)
public class NotificationSchedule extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long recipientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType notificationType;

    @Column(nullable = false)
    private Long referenceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationChannel channel;

    @Column(nullable = false)
    private LocalDateTime scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationScheduleStatus status;

    protected NotificationSchedule() {
    }

    public NotificationSchedule(
            Long recipientId,
            NotificationType notificationType,
            Long referenceId,
            NotificationChannel channel,
            LocalDateTime scheduledAt
    ) {
        this.recipientId = recipientId;
        this.notificationType = notificationType;
        this.referenceId = referenceId;
        this.channel = channel;
        this.scheduledAt = scheduledAt;
        this.status = NotificationScheduleStatus.PENDING;
    }

    public void markDispatched() {
        this.status = NotificationScheduleStatus.DISPATCHED;
    }

    public Long getId() {
        return id;
    }

    public Long getRecipientId() {
        return recipientId;
    }

    public NotificationType getNotificationType() {
        return notificationType;
    }

    public Long getReferenceId() {
        return referenceId;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public NotificationScheduleStatus getStatus() {
        return status;
    }
}
