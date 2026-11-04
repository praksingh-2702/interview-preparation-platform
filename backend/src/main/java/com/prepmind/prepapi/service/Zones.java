package com.prepmind.prepapi.service;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Parses the browser's IANA timezone (e.g. Asia/Kolkata); anything unusable falls back to UTC. */
public final class Zones {

    private Zones() {
    }

    public static ZoneId parse(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return ZoneOffset.UTC;
        }
        try {
            return ZoneId.of(timezone.trim());
        } catch (DateTimeException e) {
            return ZoneOffset.UTC;
        }
    }
}
