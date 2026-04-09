package com.changha.notification.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.changha.notification.domain.NotificationChannel;
import com.changha.notification.domain.Notification;
import com.changha.notification.domain.NotificationType;
import org.springframework.transaction.annotation.Transactional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Optional<Notification> findByIdAndRecipientId(Long id, Long recipientId);

    Optional<Notification> findByRecipientIdAndNotificationTypeAndReferenceIdAndChannel(
            Long recipientId,
            NotificationType notificationType,
            Long referenceId,
            NotificationChannel channel
    );

    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

    Page<Notification> findByRecipientIdAndReadAtIsNullOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

    Page<Notification> findByRecipientIdAndReadAtIsNotNullOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

    long countByRecipientIdAndReadAtIsNull(Long recipientId);

    @Transactional
    @Modifying
    @Query("""
        update Notification notification
           set notification.readAt = :readAt
         where notification.id = :notificationId
           and notification.recipientId = :recipientId
           and notification.readAt is null
    """)
    int markReadIfUnread(
            @Param("notificationId") Long notificationId,
            @Param("recipientId") Long recipientId,
            @Param("readAt") LocalDateTime readAt
    );
}
