package com.changha.notification.repository;

import java.time.LocalDateTime;

public interface MemberStatsRepositoryCustom {

    void incrementUnreadCount(Long memberId, LocalDateTime now);

    boolean decrementUnreadCountIfPositive(Long memberId, LocalDateTime now);
}
