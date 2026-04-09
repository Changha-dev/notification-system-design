package com.changha.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import com.changha.notification.domain.NotificationChannel;
import com.changha.notification.domain.NotificationType;
import com.changha.notification.template.NotificationTemplate;
import com.changha.notification.template.NotificationTemplateProvider;

@Component
@RequiredArgsConstructor
public class NotificationTemplateRenderer {

    private final NotificationTemplateProvider templateProvider;

    public RenderedNotificationContent render(NotificationType type, NotificationChannel channel, Long referenceId) {
        NotificationTemplate template = templateProvider.findTemplate(type, channel);
        String title = template.titleTemplate().replace("#{referenceId}", String.valueOf(referenceId));
        String body = template.bodyTemplate().replace("#{referenceId}", String.valueOf(referenceId));
        return new RenderedNotificationContent(title, body);
    }
}
