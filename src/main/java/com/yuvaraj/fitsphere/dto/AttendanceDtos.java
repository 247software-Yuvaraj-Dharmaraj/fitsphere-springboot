package com.yuvaraj.fitsphere.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class AttendanceDtos {

    private AttendanceDtos() {
    }

    public record AttendanceDto(String id, Instant checkInAt, Instant checkOutAt) {
    }

    public record Occupancy(int activeCount, int capacity, int percent, String level) {
    }

    public record Totals(int thisWeek, int thisMonth, int allTime) {
    }

    public record Summary(boolean checkedIn, Instant since, int streak,
                          Map<String, Boolean> milestones, Totals totals, Occupancy occupancy) {
    }

    public record DayPoint(String day, int present) {
    }

    public record Trend(int days, List<DayPoint> series, int attendedDays, int consistency) {
    }

    public record MonthEntry(String id, Instant checkInAt, Instant checkOutAt, String day) {
    }

    public record HourCount(int hour, int count) {
    }

    public record BestTime(List<HourCount> hours, List<HourCount> quietest, Integer suggestion) {
    }
}
