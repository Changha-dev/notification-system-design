package com.changha.notification.repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class MemberStatsRepositoryImpl implements MemberStatsRepositoryCustom {

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void incrementUnreadCount(Long memberId, LocalDateTime now) {
        jdbcTemplate.update("""
                insert into member_stats (member_id, unread_count, created_at, updated_at)
                values (?, 1, ?, ?)
                on duplicate key update unread_count = unread_count + 1, updated_at = values(updated_at)
                """,
                memberId,
                Timestamp.valueOf(now),
                Timestamp.valueOf(now)
        );
    }

    @Override
    @Transactional
    public boolean decrementUnreadCountIfPositive(Long memberId, LocalDateTime now) {
        int updated = jdbcTemplate.update("""
                update member_stats
                   set unread_count = unread_count - 1,
                       updated_at = ?
                 where member_id = ?
                   and unread_count > 0
                """,
                Timestamp.valueOf(now),
                memberId
        );
        return updated == 1;
    }
}
