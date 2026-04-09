package com.changha.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "member_stats")
public class MemberStats extends BaseTimeEntity {

    @Id
    private Long memberId;

    @Column(nullable = false)
    private int unreadCount;

    protected MemberStats() {
    }

    public MemberStats(Long memberId, int unreadCount) {
        this.memberId = memberId;
        this.unreadCount = unreadCount;
    }

    public void syncUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    public Long getMemberId() {
        return memberId;
    }

    public int getUnreadCount() {
        return unreadCount;
    }
}
