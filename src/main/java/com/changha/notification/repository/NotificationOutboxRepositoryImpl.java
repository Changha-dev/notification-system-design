package com.changha.notification.repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.changha.notification.domain.NotificationOutboxStatus;

@Repository
@RequiredArgsConstructor
public class NotificationOutboxRepositoryImpl implements NotificationOutboxRepositoryCustom {

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public boolean claimPending(Long outboxId, String workerId, LocalDateTime now) {
        int updated = jdbcTemplate.update("""
                update notification_outbox
                   set status = ?, locked_by = ?, locked_at = ?, updated_at = ?
                 where id = ?
                   and status = ?
                   and available_at <= ?
                """,
                NotificationOutboxStatus.PROCESSING.name(),
                workerId,
                Timestamp.valueOf(now),
                Timestamp.valueOf(now),
                outboxId,
                NotificationOutboxStatus.PENDING.name(),
                Timestamp.valueOf(now)
        );
        return updated == 1;
    }

    @Override
    @Transactional
    public List<Long> claimPendingBatch(String workerId, LocalDateTime now, int limit) {
        List<Long> ids = jdbcTemplate.queryForList("""
                select id
                  from notification_outbox
                 where status = ?
                   and available_at <= ?
                 order by id
                 limit ?
                 for update skip locked
                """,
                Long.class,
                NotificationOutboxStatus.PENDING.name(),
                Timestamp.valueOf(now),
                limit
        );

        if (ids.isEmpty()) {
            return Collections.emptyList();
        }

        jdbcTemplate.update("""
                update notification_outbox
                   set status = ?, locked_by = ?, locked_at = ?, updated_at = ?
                 where id in (%s)
                """.formatted(inClause(ids)),
                args(NotificationOutboxStatus.PROCESSING.name(), workerId, now, ids)
        );
        return ids;
    }

    @Override
    @Transactional
    public List<Long> requeueStaleProcessing(LocalDateTime staleThreshold, LocalDateTime now, int limit) {
        List<Long> ids = jdbcTemplate.queryForList("""
                select id
                  from notification_outbox
                 where status = ?
                   and locked_at <= ?
                 order by locked_at
                 limit ?
                 for update skip locked
                """,
                Long.class,
                NotificationOutboxStatus.PROCESSING.name(),
                Timestamp.valueOf(staleThreshold),
                limit
        );

        if (ids.isEmpty()) {
            return Collections.emptyList();
        }

        jdbcTemplate.update("""
                update notification_outbox
                   set status = ?, available_at = ?, locked_by = null, locked_at = null, updated_at = ?
                 where id in (%s)
                """.formatted(inClause(ids)),
                args(NotificationOutboxStatus.PENDING.name(), now, now, ids)
        );
        return ids;
    }

    private String inClause(List<Long> ids) {
        return ids.stream().map(id -> "?").collect(Collectors.joining(","));
    }

    private Object[] args(String status, String workerId, LocalDateTime now, List<Long> ids) {
        Object[] args = new Object[4 + ids.size()];
        args[0] = status;
        args[1] = workerId;
        args[2] = Timestamp.valueOf(now);
        args[3] = Timestamp.valueOf(now);
        for (int index = 0; index < ids.size(); index++) {
            args[4 + index] = ids.get(index);
        }
        return args;
    }

    private Object[] args(String status, LocalDateTime availableAt, LocalDateTime now, List<Long> ids) {
        Object[] args = new Object[3 + ids.size()];
        args[0] = status;
        args[1] = Timestamp.valueOf(availableAt);
        args[2] = Timestamp.valueOf(now);
        for (int index = 0; index < ids.size(); index++) {
            args[3 + index] = ids.get(index);
        }
        return args;
    }
}
