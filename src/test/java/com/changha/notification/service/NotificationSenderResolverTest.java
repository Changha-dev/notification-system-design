package com.changha.notification.service;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.changha.notification.domain.Notification;
import com.changha.notification.domain.NotificationChannel;
import com.changha.notification.domain.NotificationStatus;
import com.changha.notification.domain.NotificationType;
import com.changha.notification.sender.NotificationSender;
import com.changha.notification.sender.NotificationSenderResolver;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationSenderResolverTest {

    @Test
    void resolveShouldReturnSenderMatchedByChannel() {
        NotificationSender emailSender = new StubSender(NotificationChannel.EMAIL);
        NotificationSender inAppSender = new StubSender(NotificationChannel.IN_APP);
        NotificationSenderResolver resolver = new NotificationSenderResolver(List.of(emailSender, inAppSender));

        assertThat(resolver.resolve(NotificationChannel.EMAIL)).isSameAs(emailSender);
        assertThat(resolver.resolve(NotificationChannel.IN_APP)).isSameAs(inAppSender);
    }

    private record StubSender(NotificationChannel channel) implements NotificationSender {
        @Override
        public void send(Notification notification, String deliveryKey) {
        }
    }
}
