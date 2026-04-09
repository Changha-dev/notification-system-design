package com.changha.notification.repository;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationOutboxRepositoryCustom {

    boolean claimPending(Long outboxId, String workerId, LocalDateTime now);

    List<Long> claimPendingBatch(String workerId, LocalDateTime now, int limit);

    List<Long> requeueStaleProcessing(LocalDateTime staleThreshold, LocalDateTime now, int limit);
}
