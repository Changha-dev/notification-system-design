package com.changha.notification.template;

import org.junit.jupiter.api.Test;

import com.changha.notification.domain.NotificationChannel;
import com.changha.notification.domain.NotificationType;
import com.changha.notification.service.NotificationTemplateRenderer;
import com.changha.notification.service.RenderedNotificationContent;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationTemplateProviderTest {

    @Test
    void renderShouldReplaceReferencePlaceholder() {
        NotificationTemplateRenderer renderer = new NotificationTemplateRenderer(new SimpleNotificationTemplateProvider());

        RenderedNotificationContent content = renderer.render(
                NotificationType.PAYMENT_CONFIRMED,
                NotificationChannel.EMAIL,
                5001L
        );

        assertThat(content.title()).isEqualTo("결제 완료 안내");
        assertThat(content.body()).contains("5001");
    }
}
