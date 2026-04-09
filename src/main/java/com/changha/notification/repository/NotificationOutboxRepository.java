package com.changha.notification.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.changha.notification.domain.NotificationOutbox;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, Long>, NotificationOutboxRepositoryCustom {

    @EntityGraph(attributePaths = "notification")
    Optional<NotificationOutbox> findWithNotificationById(Long id);

    @EntityGraph(attributePaths = "notification")
    Optional<NotificationOutbox> findByNotificationId(Long notificationId);
}
