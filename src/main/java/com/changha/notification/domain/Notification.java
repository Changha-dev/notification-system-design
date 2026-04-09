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
        name = "notifications",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_notification_idempotency",
                        columnNames = {"recipient_id", "notification_type", "reference_id", "channel"}
                )
        },
        indexes = {
                @Index(name = "idx_notifications_recipient_read_created", columnList = "recipient_id, read_at, created_at")
        }
)
public class Notification extends BaseTimeEntity {

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    private LocalDateTime sentAt;

    private LocalDateTime readAt;

    protected Notification() {
    }

    public Notification(
            Long recipientId,
            NotificationType notificationType,
            Long referenceId,
            NotificationChannel channel,
            NotificationStatus status,
            String title,
            String body
    ) {
        this.recipientId = recipientId;
        this.notificationType = notificationType;
        this.referenceId = referenceId;
        this.channel = channel;
        this.status = status;
        this.title = title;
        this.body = body;
    }

    public void markSending() {
        this.status = NotificationStatus.SENDING;
    }

    public void markSent(LocalDateTime sentAt) {
        this.status = NotificationStatus.SENT;
        this.sentAt = sentAt;
    }

    public void markPending() {
        this.status = NotificationStatus.PENDING;
    }

    public void markFailed() {
        this.status = NotificationStatus.FAILED;
    }

    public boolean markRead(LocalDateTime readAt) {
        if (this.readAt != null) {
            return false;
        }
        this.readAt = readAt;
        return true;
    }

    public boolean isRead() {
        return readAt != null;
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

    public NotificationStatus getStatus() {
        return status;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }
}
