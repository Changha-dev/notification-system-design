package com.changha.notification.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;

import com.changha.notification.domain.Notification;
import com.changha.notification.domain.NotificationChannel;
import com.changha.notification.domain.NotificationOutbox;
import com.changha.notification.domain.NotificationSchedule;
import com.changha.notification.domain.NotificationScheduleStatus;
import com.changha.notification.domain.NotificationStatus;
import com.changha.notification.domain.NotificationType;
import com.changha.notification.dto.CreateNotificationRequest;
import com.changha.notification.dto.NotificationAcceptedResponse;
import com.changha.notification.dto.NotificationDetailResponse;
import com.changha.notification.dto.NotificationListResponse;
import com.changha.notification.dto.NotificationSummaryResponse;
import com.changha.notification.dto.ReadFilter;
import com.changha.notification.dto.ReadNotificationResponse;
import com.changha.notification.event.NotificationOutboxCreatedEvent;
import com.changha.notification.repository.NotificationOutboxRepository;
import com.changha.notification.repository.NotificationRepository;
import com.changha.notification.repository.NotificationScheduleRepository;

@Service
public class NotificationApplicationService {

    private final NotificationRepository notificationRepository;
    private final NotificationOutboxRepository notificationOutboxRepository;
    private final NotificationScheduleRepository notificationScheduleRepository;
    private final NotificationTemplateRenderer templateRenderer;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public NotificationApplicationService(
            NotificationRepository notificationRepository,
            NotificationOutboxRepository notificationOutboxRepository,
            NotificationScheduleRepository notificationScheduleRepository,
            NotificationTemplateRenderer templateRenderer,
            ApplicationEventPublisher eventPublisher,
            Clock clock
    ) {
        this.notificationRepository = notificationRepository;
        this.notificationOutboxRepository = notificationOutboxRepository;
        this.notificationScheduleRepository = notificationScheduleRepository;
        this.templateRenderer = templateRenderer;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public NotificationAcceptedResponse accept(CreateNotificationRequest request) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (request.scheduleAt() != null && request.scheduleAt().isAfter(now)) {
            return createSchedule(request);
        }
        return createImmediate(request.recipientId(), request.notificationType(), request.channel(), request.referenceId());
    }

    @Transactional(readOnly = true)
    public NotificationDetailResponse getDetail(Long notificationId, Long recipientId) {
        Notification notification = notificationRepository.findByIdAndRecipientId(notificationId, recipientId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId, recipientId));
        Optional<com.changha.notification.domain.NotificationOutbox> outbox =
                notificationOutboxRepository.findByNotificationId(notification.getId());

        return new NotificationDetailResponse(
                notification.getId(),
                notification.getRecipientId(),
                notification.getNotificationType(),
                notification.getChannel(),
                notification.getReferenceId(),
                notification.getStatus(),
                notification.isRead(),
                notification.getReadAt(),
                notification.getSentAt(),
                outbox.map(com.changha.notification.domain.NotificationOutbox::getLastFailureCode).orElse(null),
                outbox.map(com.changha.notification.domain.NotificationOutbox::getLastFailureMessage).orElse(null),
                notification.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public NotificationListResponse getList(Long recipientId, ReadFilter readFilter, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notifications = switch (readFilter) {
            case READ -> notificationRepository.findByRecipientIdAndReadAtIsNotNullOrderByCreatedAtDesc(recipientId, pageable);
            case UNREAD -> notificationRepository.findByRecipientIdAndReadAtIsNullOrderByCreatedAtDesc(recipientId, pageable);
            case ALL -> notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId, pageable);
        };

        List<NotificationSummaryResponse> content = notifications.stream()
                .map(notification -> new NotificationSummaryResponse(
                        notification.getId(),
                        notification.getNotificationType(),
                        notification.getChannel(),
                        notification.getStatus(),
                        notification.isRead(),
                        notification.getReadAt(),
                        notification.getSentAt(),
                        notification.getCreatedAt()
                ))
                .toList();
        return new NotificationListResponse(content, page, size, notifications.hasNext());
    }

    @Transactional
    public ReadNotificationResponse markRead(Long notificationId, Long recipientId) {
        LocalDateTime now = LocalDateTime.now(clock);
        notificationRepository.markReadIfUnread(notificationId, recipientId, now);
        Notification notification = notificationRepository.findByIdAndRecipientId(notificationId, recipientId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId, recipientId));
        return new ReadNotificationResponse(notification.getId(), notification.isRead(), notification.getReadAt());
    }

    @Transactional
    public boolean dispatchScheduledNotification(Long scheduleId) {
        NotificationSchedule schedule = notificationScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found. scheduleId=" + scheduleId));

        if (schedule.getStatus() != NotificationScheduleStatus.PENDING) {
            return false;
        }

        createImmediate(schedule.getRecipientId(), schedule.getNotificationType(), schedule.getChannel(), schedule.getReferenceId());
        schedule.markDispatched();
        return true;
    }

    private NotificationAcceptedResponse createSchedule(CreateNotificationRequest request) {
        LocalDateTime scheduleAt = request.scheduleAt();
        Optional<NotificationSchedule> existing = notificationScheduleRepository
                .findByRecipientIdAndNotificationTypeAndReferenceIdAndChannelAndScheduledAt(
                        request.recipientId(),
                        request.notificationType(),
                        request.referenceId(),
                        request.channel(),
                        scheduleAt
                );
        if (existing.isPresent()) {
            return new NotificationAcceptedResponse(null, existing.get().getId(), existing.get().getStatus().name(), true,
                    "알림 예약 요청이 접수되었습니다.");
        }

        try {
            NotificationSchedule schedule = notificationScheduleRepository.save(new NotificationSchedule(
                    request.recipientId(),
                    request.notificationType(),
                    request.referenceId(),
                    request.channel(),
                    scheduleAt
            ));
            return new NotificationAcceptedResponse(null, schedule.getId(), schedule.getStatus().name(), true,
                    "알림 예약 요청이 접수되었습니다.");
        } catch (DataIntegrityViolationException exception) {
            NotificationSchedule schedule = notificationScheduleRepository
                    .findByRecipientIdAndNotificationTypeAndReferenceIdAndChannelAndScheduledAt(
                            request.recipientId(),
                            request.notificationType(),
                            request.referenceId(),
                            request.channel(),
                            scheduleAt
                    )
                    .orElseThrow(() -> exception);
            return new NotificationAcceptedResponse(null, schedule.getId(), schedule.getStatus().name(), true,
                    "알림 예약 요청이 접수되었습니다.");
        }
    }

    private NotificationAcceptedResponse createImmediate(
            Long recipientId,
            NotificationType notificationType,
            NotificationChannel channel,
            Long referenceId
    ) {
        Optional<Notification> existing = notificationRepository.findByRecipientIdAndNotificationTypeAndReferenceIdAndChannel(
                recipientId,
                notificationType,
                referenceId,
                channel
        );
        if (existing.isPresent()) {
            return new NotificationAcceptedResponse(existing.get().getId(), null, existing.get().getStatus().name(), true,
                    "알림 요청이 접수되었습니다.");
        }

        try {
            RenderedNotificationContent content = templateRenderer.render(notificationType, channel, referenceId);
            Notification notification = notificationRepository.save(new Notification(
                    recipientId,
                    notificationType,
                    referenceId,
                    channel,
                    NotificationStatus.PENDING,
                    content.title(),
                    content.body()
            ));
            NotificationOutbox outbox = notificationOutboxRepository.save(new NotificationOutbox(notification, LocalDateTime.now(clock)));
            eventPublisher.publishEvent(new NotificationOutboxCreatedEvent(outbox.getId()));
            return new NotificationAcceptedResponse(notification.getId(), null, notification.getStatus().name(), true,
                    "알림 요청이 접수되었습니다.");
        } catch (DataIntegrityViolationException exception) {
            Notification notification = notificationRepository.findByRecipientIdAndNotificationTypeAndReferenceIdAndChannel(
                    recipientId,
                    notificationType,
                    referenceId,
                    channel
            ).orElseThrow(() -> exception);
            return new NotificationAcceptedResponse(notification.getId(), null, notification.getStatus().name(), true,
                    "알림 요청이 접수되었습니다.");
        }
    }
}
