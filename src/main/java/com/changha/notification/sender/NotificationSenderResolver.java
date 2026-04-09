package com.changha.notification.sender;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.changha.notification.domain.NotificationChannel;

@Component
public class NotificationSenderResolver {

    private final Map<NotificationChannel, NotificationSender> senders = new EnumMap<>(NotificationChannel.class);

    public NotificationSenderResolver(List<NotificationSender> senders) {
        for (NotificationSender sender : senders) {
            this.senders.put(sender.channel(), sender);
        }
    }

    public NotificationSender resolve(NotificationChannel channel) {
        NotificationSender sender = senders.get(channel);
        if (sender == null) {
            throw new IllegalArgumentException("Sender not found for channel=" + channel);
        }
        return sender;
    }
}
