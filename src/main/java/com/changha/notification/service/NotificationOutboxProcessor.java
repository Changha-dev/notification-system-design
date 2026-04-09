package com.changha.notification.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.changha.notification.config.NotificationProperties;
import com.changha.notification.domain.Notification;
import com.changha.notification.domain.NotificationOutbox;
import com.changha.notification.domain.NotificationOutboxStatus;
import com.changha.notification.repository.NotificationOutboxRepository;
import com.changha.notification.sender.NotificationDispatchException;
import com.changha.notification.sender.NotificationSenderResolver;
import com.changha.notification.sender.PermanentNotificationDispatchException;
import com.changha.notification.sender.RetryableNotificationDispatchException;

@Service
public class NotificationOutboxProcessor {

    private final NotificationOutboxRepository outboxRepository;
    private final NotificationSenderResolver senderResolver;
    private final TransactionTemplate transactionTemplate;
    private final NotificationProperties notificationProperties;
    private final Clock clock;

    public NotificationOutboxProcessor(
            NotificationOutboxRepository outboxRepository,
            NotificationSenderResolver senderResolver,
            PlatformTransactionManager transactionManager,
            NotificationProperties notificationProperties,
            Clock clock
    ) {
        this.outboxRepository = outboxRepository;
        this.senderResolver = senderResolver;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.notificationProperties = notificationProperties;
        this.clock = clock;
    }

    public boolean processPendingOutbox(Long outboxId, String workerId) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (!outboxRepository.claimPending(outboxId, workerId, now)) {
            return false;
        }
        processClaimedOutbox(outboxId);
        return true;
    }

    public boolean processClaimedOutbox(Long outboxId) {
        Notification notification = transactionTemplate.execute(status -> {
            NotificationOutbox outbox = outboxRepository.findWithNotificationById(outboxId)
                    .orElseThrow(() -> new IllegalArgumentException("Outbox not found. outboxId=" + outboxId));
            if (outbox.getStatus() != NotificationOutboxStatus.PROCESSING) {
                return null;
            }
            Notification notificationEntity = outbox.getNotification();
            notificationEntity.markSending();
            return notificationEntity;
        });

        if (notification == null) {
            return false;
        }

        try {
            senderResolver.resolve(notification.getChannel()).send(notification, String.valueOf(notification.getId()));
            markSuccess(outboxId);
        } catch (RetryableNotificationDispatchException exception) {
            handleRetryableFailure(outboxId, exception);
        } catch (PermanentNotificationDispatchException exception) {
            markDead(outboxId, exception);
        } catch (NotificationDispatchException exception) {
            markDead(outboxId, exception);
        } catch (RuntimeException exception) {
            handleRetryableFailure(outboxId, new RetryableNotificationDispatchException("UNEXPECTED_ERROR", exception.getMessage()));
        }
        return true;
    }

    private void markSuccess(Long outboxId) {
        LocalDateTime now = LocalDateTime.now(clock);
        transactionTemplate.executeWithoutResult(status -> {
            NotificationOutbox outbox = outboxRepository.findWithNotificationById(outboxId)
                    .orElseThrow(() -> new IllegalArgumentException("Outbox not found. outboxId=" + outboxId));
            outbox.getNotification().markSent(now);
            outbox.markCompleted();
        });
    }

    private void handleRetryableFailure(Long outboxId, RetryableNotificationDispatchException exception) {
        LocalDateTime now = LocalDateTime.now(clock);
        transactionTemplate.executeWithoutResult(status -> {
            NotificationOutbox outbox = outboxRepository.findWithNotificationById(outboxId)
                    .orElseThrow(() -> new IllegalArgumentException("Outbox not found. outboxId=" + outboxId));
            int nextAttempt = outbox.getRetryCount() + 1;
            if (nextAttempt >= notificationProperties.retry().maxAttempts()) {
                outbox.getNotification().markFailed();
                outbox.markDead(exception.getFailureCode(), exception.getMessage());
                return;
            }
            long delaySeconds = notificationProperties.retry().baseDelaySeconds() * (1L << outbox.getRetryCount());
            outbox.getNotification().markPending();
            outbox.markRetry(now.plusSeconds(delaySeconds), exception.getFailureCode(), exception.getMessage());
        });
    }

    private void markDead(Long outboxId, NotificationDispatchException exception) {
        transactionTemplate.executeWithoutResult(status -> {
            NotificationOutbox outbox = outboxRepository.findWithNotificationById(outboxId)
                    .orElseThrow(() -> new IllegalArgumentException("Outbox not found. outboxId=" + outboxId));
            outbox.getNotification().markFailed();
            outbox.markDead(exception.getFailureCode(), exception.getMessage());
        });
    }
}
