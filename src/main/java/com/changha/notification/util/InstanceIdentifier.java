package com.changha.notification.util;

import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class InstanceIdentifier {

    private final String id = UUID.randomUUID().toString();

    public String currentId() {
        return id;
    }
}
