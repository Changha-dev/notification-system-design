package com.changha.notification.config;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import com.changha.notification.domain.MemberStats;
import com.changha.notification.domain.Notification;
import com.changha.notification.domain.NotificationChannel;
import com.changha.notification.domain.NotificationOutbox;
import com.changha.notification.domain.NotificationSchedule;
import com.changha.notification.domain.NotificationStatus;
import com.changha.notification.domain.NotificationType;
import com.changha.notification.repository.MemberStatsRepository;
import com.changha.notification.repository.NotificationOutboxRepository;
import com.changha.notification.repository.NotificationRepository;
import com.changha.notification.repository.NotificationScheduleRepository;
import com.changha.notification.service.NotificationTemplateRenderer;
import com.changha.notification.service.RenderedNotificationContent;

@Configuration
public class LocalSampleDataInitializer {

    @Bean
    @ConditionalOnProperty(name = "app.notification.seed.enabled", havingValue = "true")
    ApplicationRunner localSeedRunner(LocalSampleDataSeeder seeder) {
        return args -> seeder.seed();
    }

    @Bean
    @ConditionalOnProperty(name = "app.notification.seed.enabled", havingValue = "true")
    LocalSampleDataSeeder localSampleDataSeeder(
            NotificationRepository notificationRepository,
            NotificationOutboxRepository notificationOutboxRepository,
            NotificationScheduleRepository notificationScheduleRepository,
            MemberStatsRepository memberStatsRepository,
            NotificationTemplateRenderer templateRenderer,
            Clock clock
    ) {
        return new LocalSampleDataSeeder(
                notificationRepository,
                notificationOutboxRepository,
                notificationScheduleRepository,
                memberStatsRepository,
                templateRenderer,
                clock
        );
    }

    static class LocalSampleDataSeeder {

        private final NotificationRepository notificationRepository;
        private final NotificationOutboxRepository notificationOutboxRepository;
        private final NotificationScheduleRepository notificationScheduleRepository;
        private final MemberStatsRepository memberStatsRepository;
        private final NotificationTemplateRenderer templateRenderer;
        private final Clock clock;

        LocalSampleDataSeeder(
                NotificationRepository notificationRepository,
                NotificationOutboxRepository notificationOutboxRepository,
                NotificationScheduleRepository notificationScheduleRepository,
                MemberStatsRepository memberStatsRepository,
                NotificationTemplateRenderer templateRenderer,
                Clock clock
        ) {
            this.notificationRepository = notificationRepository;
            this.notificationOutboxRepository = notificationOutboxRepository;
            this.notificationScheduleRepository = notificationScheduleRepository;
            this.memberStatsRepository = memberStatsRepository;
            this.templateRenderer = templateRenderer;
            this.clock = clock;
        }

        @Transactional
        public void seed() {
            if (notificationRepository.count() > 0 || notificationScheduleRepository.count() > 0 || memberStatsRepository.count() > 0) {
                return;
            }

            LocalDateTime now = LocalDateTime.now(clock);

            Notification unreadEmail = createCompletedNotification(
                    1001L,
                    NotificationType.PAYMENT_CONFIRMED,
                    NotificationChannel.EMAIL,
                    5001L,
                    now.minusHours(2),
                    null
            );
            Notification readInApp = createCompletedNotification(
                    1001L,
                    NotificationType.COURSE_START_D_MINUS_1,
                    NotificationChannel.IN_APP,
                    7001L,
                    now.minusDays(1),
                    now.minusHours(12)
            );
            Notification unreadInApp = createCompletedNotification(
                    1002L,
                    NotificationType.ENROLLMENT_COMPLETED,
                    NotificationChannel.IN_APP,
                    8001L,
                    now.minusMinutes(30),
                    null
            );

            notificationRepository.save(unreadEmail);
            notificationRepository.save(readInApp);
            notificationRepository.save(unreadInApp);

            notificationOutboxRepository.save(completedOutbox(unreadEmail, now.minusHours(2)));
            notificationOutboxRepository.save(completedOutbox(readInApp, now.minusDays(1)));
            notificationOutboxRepository.save(completedOutbox(unreadInApp, now.minusMinutes(30)));

            notificationScheduleRepository.save(new NotificationSchedule(
                    1001L,
                    NotificationType.ENROLLMENT_CANCELLED,
                    9001L,
                    NotificationChannel.EMAIL,
                    now.plusMinutes(20)
            ));

            memberStatsRepository.save(new MemberStats(1001L, 1));
            memberStatsRepository.save(new MemberStats(1002L, 1));
        }

        private Notification createCompletedNotification(
                Long recipientId,
                NotificationType notificationType,
                NotificationChannel channel,
                Long referenceId,
                LocalDateTime sentAt,
                LocalDateTime readAt
        ) {
            RenderedNotificationContent content = templateRenderer.render(notificationType, channel, referenceId);
            Notification notification = new Notification(
                    recipientId,
                    notificationType,
                    referenceId,
                    channel,
                    NotificationStatus.PENDING,
                    content.title(),
                    content.body()
            );
            notification.markSent(sentAt);
            if (readAt != null) {
                notification.markRead(readAt);
            }
            return notification;
        }

        private NotificationOutbox completedOutbox(Notification notification, LocalDateTime completedAt) {
            NotificationOutbox outbox = new NotificationOutbox(notification, completedAt);
            outbox.markCompleted();
            return outbox;
        }
    }
}
