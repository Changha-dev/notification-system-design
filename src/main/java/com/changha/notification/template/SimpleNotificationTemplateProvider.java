package com.changha.notification.template;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.changha.notification.domain.NotificationChannel;
import com.changha.notification.domain.NotificationType;

@Component
public class SimpleNotificationTemplateProvider implements NotificationTemplateProvider {

    private final Map<String, NotificationTemplate> templates = Map.ofEntries(
            Map.entry(key(NotificationType.PAYMENT_CONFIRMED, NotificationChannel.EMAIL),
                    new NotificationTemplate("결제 완료 안내", "결제 #{referenceId}가 정상적으로 완료되었습니다.")),
            Map.entry(key(NotificationType.PAYMENT_CONFIRMED, NotificationChannel.IN_APP),
                    new NotificationTemplate("결제 완료", "결제 #{referenceId}가 완료되었습니다.")),
            Map.entry(key(NotificationType.ENROLLMENT_COMPLETED, NotificationChannel.EMAIL),
                    new NotificationTemplate("수강 신청 완료 안내", "수강 신청 #{referenceId}가 완료되었습니다.")),
            Map.entry(key(NotificationType.ENROLLMENT_COMPLETED, NotificationChannel.IN_APP),
                    new NotificationTemplate("수강 신청 완료", "수강 신청 #{referenceId}가 완료되었습니다.")),
            Map.entry(key(NotificationType.COURSE_START_D_MINUS_1, NotificationChannel.EMAIL),
                    new NotificationTemplate("강의 시작 안내", "강의 일정 #{referenceId} 시작이 하루 남았습니다.")),
            Map.entry(key(NotificationType.COURSE_START_D_MINUS_1, NotificationChannel.IN_APP),
                    new NotificationTemplate("강의 시작 D-1", "강의 일정 #{referenceId} 시작이 하루 남았습니다.")),
            Map.entry(key(NotificationType.ENROLLMENT_CANCELLED, NotificationChannel.EMAIL),
                    new NotificationTemplate("수강 취소 안내", "수강 취소 #{referenceId}가 반영되었습니다.")),
            Map.entry(key(NotificationType.ENROLLMENT_CANCELLED, NotificationChannel.IN_APP),
                    new NotificationTemplate("수강 취소 완료", "수강 취소 #{referenceId}가 반영되었습니다."))
    );

    @Override
    public NotificationTemplate findTemplate(NotificationType notificationType, NotificationChannel channel) {
        NotificationTemplate template = templates.get(key(notificationType, channel));
        if (template == null) {
            throw new IllegalArgumentException("Template not found for type=%s channel=%s".formatted(notificationType, channel));
        }
        return template;
    }

    private static String key(NotificationType type, NotificationChannel channel) {
        return type.name() + ":" + channel.name();
    }
}
