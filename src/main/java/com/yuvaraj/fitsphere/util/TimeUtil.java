package com.yuvaraj.fitsphere.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

/** UTC day-bucket helpers — keeps streak/trend math timezone-stable (mirrors the Node helpers). */
public final class TimeUtil {

    public static final ZoneOffset UTC = ZoneOffset.UTC;

    private TimeUtil() {
    }

    public static Instant startOfDay() {
        return startOfDay(Instant.now());
    }

    public static Instant startOfDay(Instant t) {
        return t.atZone(UTC).toLocalDate().atStartOfDay(UTC).toInstant();
    }

    /** Sunday-based week start (matches JS getUTCDay where Sunday = 0). */
    public static Instant startOfWeek() {
        LocalDate today = LocalDate.now(UTC);
        LocalDate sunday = today.minusDays(today.getDayOfWeek().getValue() % 7);
        return sunday.atStartOfDay(UTC).toInstant();
    }

    public static Instant startOfMonth() {
        return LocalDate.now(UTC).withDayOfMonth(1).atStartOfDay(UTC).toInstant();
    }

    public static Instant daysAgo(int n) {
        return startOfDay().minus(n, ChronoUnit.DAYS);
    }

    public static String dayKey(Instant t) {
        return t.atZone(UTC).toLocalDate().toString();
    }

    public static int hourOf(Instant t) {
        return t.atZone(UTC).getHour();
    }
}
