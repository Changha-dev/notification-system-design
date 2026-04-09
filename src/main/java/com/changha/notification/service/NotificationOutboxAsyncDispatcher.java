package com.changha.notification.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.changha.notification.config.NotificationProperties;
import com.changha.notification.event.NotificationOutboxCreatedEvent;
import com.changha.notification.util.InstanceIdentifier;
import com.changha.notification.util.NotificationDispatchExecutor;

@Component
public class NotificationOutboxAsyncDispatcher {

    private final NotificationProperties notificationProperties;
    private final NotificationOutboxProcessor notificationOutboxProcessor;
    private final NotificationDispatchExecutor dispatchExecutor;
    private final InstanceIdentifier instanceIdentifier;

    public NotificationOutboxAsyncDispatcher(
            NotificationProperties notificationProperties,
            NotificationOutboxProcessor notificationOutboxProcessor,
            NotificationDispatchExecutor dispatchExecutor,
            InstanceIdentifier instanceIdentifier
    ) {
        this.notificationProperties = notificationProperties;
        this.notificationOutboxProcessor = notificationOutboxProcessor;
        this.dispatchExecutor = dispatchExecutor;
        this.instanceIdentifier = instanceIdentifier;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(NotificationOutboxCreatedEvent event) {
        if (!notificationProperties.autoDispatchEnabled()) {
            return;
        }
        String workerId = "async-" + instanceIdentifier.currentId();
        dispatchExecutor.execute(() -> notificationOutboxProcessor.processPendingOutbox(event.outboxId(), workerId));
    }
}
