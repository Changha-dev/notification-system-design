package com.changha.notification.template;

import com.changha.notification.domain.NotificationChannel;
import com.changha.notification.domain.NotificationType;

public interface NotificationTemplateProvider {

    NotificationTemplate findTemplate(NotificationType notificationType, NotificationChannel channel);
}
