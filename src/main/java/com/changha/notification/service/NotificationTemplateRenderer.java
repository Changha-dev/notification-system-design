package com.changha.notification.service;

import org.springframework.stereotype.Component;

import com.changha.notification.domain.NotificationChannel;
import com.changha.notification.domain.NotificationType;
import com.changha.notification.template.NotificationTemplate;
import com.changha.notification.template.NotificationTemplateProvider;

@Component
public class NotificationTemplateRenderer {

    private final NotificationTemplateProvider templateProvider;

    public NotificationTemplateRenderer(NotificationTemplateProvider templateProvider) {
        this.templateProvider = templateProvider;
    }

    public RenderedNotificationContent render(NotificationType type, NotificationChannel channel, Long referenceId) {
        NotificationTemplate template = templateProvider.findTemplate(type, channel);
        String title = template.titleTemplate().replace("#{referenceId}", String.valueOf(referenceId));
        String body = template.bodyTemplate().replace("#{referenceId}", String.valueOf(referenceId));
        return new RenderedNotificationContent(title, body);
    }
}
