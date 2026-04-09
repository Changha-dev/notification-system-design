package com.changha.notification.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(
        name = "notification_outbox",
        indexes = {
                @Index(name = "idx_outbox_status_available", columnList = "status, available_at, id"),
                @Index(name = "idx_outbox_status_locked", columnList = "status, locked_at, id")
        }
)
public class NotificationOutbox extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_id", nullable = false, unique = true)
    private Notification notification;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationOutboxStatus status;

    @Column(nullable = false)
    private int retryCount;

    @Column(nullable = false)
    private LocalDateTime availableAt;

    @Column(length = 100)
    private String lockedBy;

    private LocalDateTime lockedAt;

    @Column(length = 100)
    private String lastFailureCode;

    @Column(columnDefinition = "TEXT")
    private String lastFailureMessage;

    protected NotificationOutbox() {
    }

    public NotificationOutbox(Notification notification, LocalDateTime availableAt) {
        this.notification = notification;
        this.status = NotificationOutboxStatus.PENDING;
        this.retryCount = 0;
        this.availableAt = availableAt;
    }

    public void markCompleted() {
        this.status = NotificationOutboxStatus.COMPLETED;
        this.lockedAt = null;
        this.lockedBy = null;
    }

    public void markRetry(LocalDateTime nextAvailableAt, String failureCode, String failureMessage) {
        this.status = NotificationOutboxStatus.PENDING;
        this.retryCount += 1;
        this.availableAt = nextAvailableAt;
        this.lastFailureCode = failureCode;
        this.lastFailureMessage = failureMessage;
        this.lockedAt = null;
        this.lockedBy = null;
    }

    public void markDead(String failureCode, String failureMessage) {
        this.status = NotificationOutboxStatus.DEAD;
        this.lastFailureCode = failureCode;
        this.lastFailureMessage = failureMessage;
        this.lockedAt = null;
        this.lockedBy = null;
    }

    public void reclaim(LocalDateTime now) {
        this.status = NotificationOutboxStatus.PENDING;
        this.availableAt = now;
        this.lockedAt = null;
        this.lockedBy = null;
    }

    public Long getId() {
        return id;
    }

    public Notification getNotification() {
        return notification;
    }

    public NotificationOutboxStatus getStatus() {
        return status;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public LocalDateTime getAvailableAt() {
        return availableAt;
    }

    public String getLockedBy() {
        return lockedBy;
    }

    public LocalDateTime getLockedAt() {
        return lockedAt;
    }

    public String getLastFailureCode() {
        return lastFailureCode;
    }

    public String getLastFailureMessage() {
        return lastFailureMessage;
    }
}
