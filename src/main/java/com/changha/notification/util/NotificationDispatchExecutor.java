package com.changha.notification.util;

import java.util.concurrent.CompletableFuture;

import org.springframework.core.task.AsyncTaskExecutor;

public class NotificationDispatchExecutor {

    private final String type;
    private final AsyncTaskExecutor delegate;

    public NotificationDispatchExecutor(String type, AsyncTaskExecutor delegate) {
        this.type = type;
        this.delegate = delegate;
    }

    public CompletableFuture<Void> execute(Runnable task) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        delegate.execute(() -> {
            try {
                task.run();
                future.complete(null);
            } catch (Exception exception) {
                future.completeExceptionally(exception);
            }
        });
        return future;
    }

    public String type() {
        return type;
    }
}
