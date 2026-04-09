package com.changha.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.changha.notification.config.NotificationProperties;
import com.changha.notification.event.NotificationOutboxCreatedEvent;
import com.changha.notification.util.InstanceIdentifier;
import com.changha.notification.util.NotificationDispatchExecutor;

@Component
@RequiredArgsConstructor
public class NotificationOutboxAsyncDispatcher {

    private final NotificationProperties notificationProperties;
    private final NotificationOutboxProcessor notificationOutboxProcessor;
    private final NotificationDispatchExecutor dispatchExecutor;
    private final InstanceIdentifier instanceIdentifier;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(NotificationOutboxCreatedEvent event) {
        if (!notificationProperties.autoDispatchEnabled()) {
            return;
        }
        String workerId = "async-" + instanceIdentifier.currentId();
        dispatchExecutor.execute(() -> notificationOutboxProcessor.processPendingOutbox(event.outboxId(), workerId));
    }
}
