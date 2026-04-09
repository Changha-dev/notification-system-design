package com.changha.notification.schedule;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.changha.notification.config.NotificationProperties;
import com.changha.notification.repository.NotificationOutboxRepository;
import com.changha.notification.service.NotificationOutboxProcessor;
import com.changha.notification.util.InstanceIdentifier;

@Component
public class NotificationRecoveryScheduler {

    private final NotificationProperties notificationProperties;
    private final NotificationOutboxRepository outboxRepository;
    private final NotificationOutboxProcessor notificationOutboxProcessor;
    private final InstanceIdentifier instanceIdentifier;
    private final Clock clock;

    public NotificationRecoveryScheduler(
            NotificationProperties notificationProperties,
            NotificationOutboxRepository outboxRepository,
            NotificationOutboxProcessor notificationOutboxProcessor,
            InstanceIdentifier instanceIdentifier,
            Clock clock
    ) {
        this.notificationProperties = notificationProperties;
        this.outboxRepository = outboxRepository;
        this.notificationOutboxProcessor = notificationOutboxProcessor;
        this.instanceIdentifier = instanceIdentifier;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "5000")
    public void scheduledRecoverAndDispatch() {
        recoverAndDispatch();
    }

    public int recoverAndDispatch() {
        LocalDateTime now = LocalDateTime.now(clock);
        outboxRepository.requeueStaleProcessing(
                now.minusSeconds(notificationProperties.recovery().leaseTimeoutSeconds()),
                now,
                notificationProperties.recovery().batchSize()
        );

        List<Long> claimedIds = outboxRepository.claimPendingBatch(
                "scheduler-" + instanceIdentifier.currentId(),
                now,
                notificationProperties.recovery().batchSize()
        );
        claimedIds.forEach(notificationOutboxProcessor::processClaimedOutbox);
        return claimedIds.size();
    }
}
