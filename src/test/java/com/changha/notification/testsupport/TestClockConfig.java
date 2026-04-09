package com.changha.notification.testsupport;

import java.time.Clock;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestClockConfig {

    @Bean
    @Primary
    public MutableClock mutableClock() {
        return MutableClock.seoulBaseline();
    }

    @Bean
    @Primary
    public Clock clock(MutableClock mutableClock) {
        return mutableClock;
    }
}
