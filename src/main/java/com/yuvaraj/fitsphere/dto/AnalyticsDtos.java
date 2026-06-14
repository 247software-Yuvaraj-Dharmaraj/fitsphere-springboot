package com.yuvaraj.fitsphere.dto;

import com.yuvaraj.fitsphere.dto.AttendanceDtos.HourCount;
import com.yuvaraj.fitsphere.dto.AttendanceDtos.Occupancy;

import java.time.Instant;
import java.util.List;

public final class AnalyticsDtos {

    private AnalyticsDtos() {
    }

    public record Totals(long totalMembers, int activeThisWeek, Integer peakHour) {
    }

    public record DayCount(String day, int count) {
    }

    public record Overview(Totals totals, Occupancy occupancy, List<HourCount> peakHours, List<DayCount> dailyTrend) {
    }

    public record MemberRow(String id, String name, String email, int totalVisits, int thisWeek,
                            Instant lastVisit, String status) {
    }

    public record MembersResponse(List<MemberRow> rows, long total, int page, int pageSize) {
    }
}
