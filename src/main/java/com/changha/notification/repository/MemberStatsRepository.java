package com.changha.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.changha.notification.domain.MemberStats;

public interface MemberStatsRepository extends JpaRepository<MemberStats, Long>, MemberStatsRepositoryCustom {
}
