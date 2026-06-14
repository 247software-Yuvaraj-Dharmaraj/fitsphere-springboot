package com.yuvaraj.fitsphere.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

/** Day-bucket helpers. Buckets are computed at a given UTC offset (minutes east of
 *  UTC) so personal stats follow the user's local day and global metrics follow the
 *  gym's day — mirrors the Node helpers. The zero-arg variants default to UTC. */
public final class TimeUtil {

    public static final ZoneOffset UTC = ZoneOffset.UTC;

    private TimeUtil() {
    }

    /** ZoneOffset for `minutes` east of UTC (e.g. 330 = IST). */
    public static ZoneOffset offset(int minutes) {
        return ZoneOffset.ofTotalSeconds(minutes * 60);
    }

    /** Offset as a Mongo-friendly "+HH:MM" string (for $dateToString timezone). */
    public static String offsetId(int minutes) {
        return offset(minutes).getId().equals("Z") ? "+00:00" : offset(minutes).getId();
    }

    public static Instant startOfDay() {
        return startOfDay(Instant.now(), 0);
    }

    public static Instant startOfDay(int offsetMin) {
        return startOfDay(Instant.now(), offsetMin);
    }

    public static Instant startOfDay(Instant t) {
        return startOfDay(t, 0);
    }

    public static Instant startOfDay(Instant t, int offsetMin) {
        ZoneOffset z = offset(offsetMin);
        return t.atOffset(z).toLocalDate().atStartOfDay(z).toInstant();
    }

    /** Sunday-based week start (matches JS getUTCDay where Sunday = 0). */
    public static Instant startOfWeek() {
        return startOfWeek(0);
    }

    public static Instant startOfWeek(int offsetMin) {
        ZoneOffset z = offset(offsetMin);
        LocalDate today = LocalDate.now(z);
        LocalDate sunday = today.minusDays(today.getDayOfWeek().getValue() % 7);
        return sunday.atStartOfDay(z).toInstant();
    }

    public static Instant startOfMonth() {
        return startOfMonth(0);
    }

    public static Instant startOfMonth(int offsetMin) {
        ZoneOffset z = offset(offsetMin);
        return LocalDate.now(z).withDayOfMonth(1).atStartOfDay(z).toInstant();
    }

    public static Instant daysAgo(int n) {
        return startOfDay().minus(n, ChronoUnit.DAYS);
    }

    public static Instant daysAgo(int n, int offsetMin) {
        return startOfDay(offsetMin).minus(n, ChronoUnit.DAYS);
    }

    public static String dayKey(Instant t) {
        return dayKey(t, 0);
    }

    public static String dayKey(Instant t, int offsetMin) {
        return t.atOffset(offset(offsetMin)).toLocalDate().toString();
    }

    public static int hourOf(Instant t) {
        return hourOf(t, 0);
    }

    public static int hourOf(Instant t, int offsetMin) {
        return t.atOffset(offset(offsetMin)).getHour();
    }

    /** First instant (local midnight) of a YYYY-MM-DD day key at the given offset. */
    public static Instant dayStart(String dayKey, int offsetMin) {
        ZoneOffset z = offset(offsetMin);
        return LocalDate.parse(dayKey).atStartOfDay(z).toInstant();
    }

    /** UTC-midnight of the current calendar date in the given offset's timezone.
     *  For date-only "today/past" comparisons against date-only values that are
     *  themselves stored at UTC midnight (e.g. slot dates). */
    public static Instant todayDateAtUtc(int offsetMin) {
        return LocalDate.now(offset(offsetMin)).atStartOfDay(UTC).toInstant();
    }
}
