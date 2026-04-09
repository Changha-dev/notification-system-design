package com.changha.notification.testsupport;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

public class MutableClock extends Clock {

    private final ZoneId zoneId;
    private Instant instant;

    public MutableClock(Instant initialInstant, ZoneId zoneId) {
        this.instant = initialInstant;
        this.zoneId = zoneId;
    }

    @Override
    public ZoneId getZone() {
        return zoneId;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new MutableClock(instant, zone);
    }

    @Override
    public Instant instant() {
        return instant;
    }

    public LocalDateTime now() {
        return LocalDateTime.ofInstant(instant, zoneId);
    }

    public void advanceSeconds(long seconds) {
        this.instant = this.instant.plusSeconds(seconds);
    }

    public void reset(Instant newInstant) {
        this.instant = newInstant;
    }

    public static MutableClock seoulBaseline() {
        return new MutableClock(LocalDateTime.of(2026, 4, 9, 9, 0).toInstant(ZoneOffset.ofHours(9)), ZoneId.of("Asia/Seoul"));
    }
}
