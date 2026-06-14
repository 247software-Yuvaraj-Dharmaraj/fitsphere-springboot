package com.yuvaraj.fitsphere.service;

import com.yuvaraj.fitsphere.domain.Attendance;
import com.yuvaraj.fitsphere.dto.AttendanceDtos.AttendanceDto;
import com.yuvaraj.fitsphere.dto.AttendanceDtos.BestTime;
import com.yuvaraj.fitsphere.dto.AttendanceDtos.DayPoint;
import com.yuvaraj.fitsphere.dto.AttendanceDtos.HourCount;
import com.yuvaraj.fitsphere.dto.AttendanceDtos.MonthEntry;
import com.yuvaraj.fitsphere.dto.AttendanceDtos.Occupancy;
import com.yuvaraj.fitsphere.dto.AttendanceDtos.Summary;
import com.yuvaraj.fitsphere.dto.AttendanceDtos.Totals;
import com.yuvaraj.fitsphere.dto.AttendanceDtos.Trend;
import com.yuvaraj.fitsphere.exception.HttpException;
import com.yuvaraj.fitsphere.realtime.RealtimeService;
import com.yuvaraj.fitsphere.repository.AttendanceRepository;
import com.yuvaraj.fitsphere.repository.GymConfigRepository;
import com.yuvaraj.fitsphere.util.TimeUtil;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.DateOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AttendanceService {

    // Streaks longer than this aren't fully counted — >1 year at a daily cadence —
    // so we bound the history loaded per summary instead of fetching it all.
    private static final int STREAK_WINDOW_DAYS = 400;

    private final AttendanceRepository attendance;
    private final GymConfigRepository gymConfig;
    private final RealtimeService realtime;
    private final MongoTemplate mongo;

    public AttendanceService(AttendanceRepository attendance, GymConfigRepository gymConfig, RealtimeService realtime, MongoTemplate mongo) {
        this.attendance = attendance;
        this.gymConfig = gymConfig;
        this.realtime = realtime;
        this.mongo = mongo;
    }

    private int capacity() {
        return gymConfig.findAll().stream().findFirst().map(com.yuvaraj.fitsphere.domain.GymConfig::getCapacity).orElse(50);
    }

    public Occupancy getOccupancy() {
        int activeCount = (int) attendance.countByCheckOutAtIsNullAndCheckInAtGreaterThanEqual(TimeUtil.startOfDay());
        int cap = capacity();
        int percent = cap > 0 ? Math.round((activeCount / (float) cap) * 100) : 0;
        String level;
        if (percent >= 100) level = "FULL";
        else if (percent > 75) level = "HIGH";
        else if (percent > 40) level = "MEDIUM";
        else level = "LOW";
        return new Occupancy(activeCount, cap, percent, level);
    }

    public AttendanceDto checkIn(String userId) {
        Instant today = TimeUtil.startOfDay();
        attendance.findFirstByUserAndCheckOutAtIsNull(userId).ifPresent(open -> {
            if (!open.getCheckInAt().isBefore(today)) {
                throw new HttpException(HttpStatus.CONFLICT, "You are already checked in");
            }
            open.setCheckOutAt(today); // stale session from an earlier day — auto-close
            attendance.save(open);
        });

        if ("FULL".equals(getOccupancy().level())) {
            throw new HttpException(HttpStatus.CONFLICT, "The gym is at full capacity. Please try again later.");
        }

        Attendance record = attendance.save(new Attendance(userId, Instant.now()));
        realtime.emitOccupancy(getOccupancy());
        return new AttendanceDto(record.getId(), record.getCheckInAt(), record.getCheckOutAt());
    }

    public AttendanceDto checkOut(String userId) {
        Attendance open = attendance.findFirstByUserAndCheckOutAtIsNull(userId)
                .orElseThrow(() -> new HttpException(HttpStatus.BAD_REQUEST, "You are not currently checked in"));
        open.setCheckOutAt(Instant.now());
        attendance.save(open);
        realtime.emitOccupancy(getOccupancy());
        return new AttendanceDto(open.getId(), open.getCheckInAt(), open.getCheckOutAt());
    }

    private int computeStreak(Set<String> dayKeys) {
        int streak = 0;
        LocalDate cursor = LocalDate.now(ZoneOffset.UTC);
        if (!dayKeys.contains(cursor.toString())) {
            cursor = cursor.minusDays(1);
            if (!dayKeys.contains(cursor.toString())) return 0;
        }
        while (dayKeys.contains(cursor.toString())) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    public Summary getSummary(String userId) {
        Attendance open = attendance.findFirstByUserAndCheckOutAtIsNull(userId).orElse(null);
        // Load only a bounded recent window (enough for streak + this week/month)
        // rather than the full history on every summary call.
        Instant windowStart = TimeUtil.startOfDay().minus(STREAK_WINDOW_DAYS, ChronoUnit.DAYS);
        List<Attendance> recent = attendance.findByUserAndCheckInAtGreaterThanEqual(userId, windowStart);
        Occupancy occupancy = getOccupancy();

        Set<String> dayKeys = new LinkedHashSet<>();
        for (Attendance a : recent) {
            dayKeys.add(TimeUtil.dayKey(a.getCheckInAt()));
        }
        int streak = computeStreak(dayKeys);

        Instant weekStart = TimeUtil.startOfWeek();
        Instant monthStart = TimeUtil.startOfMonth();
        int thisWeek = (int) dayKeys.stream().filter(k -> !dayStart(k).isBefore(weekStart)).count();
        int thisMonth = (int) dayKeys.stream().filter(k -> !dayStart(k).isBefore(monthStart)).count();
        int allTime = countDistinctDays(userId); // server-side, never pulls full history

        boolean checkedInToday = open != null && !open.getCheckInAt().isBefore(TimeUtil.startOfDay());
        Map<String, Boolean> milestones = new HashMap<>();
        milestones.put("3", streak >= 3);
        milestones.put("7", streak >= 7);
        milestones.put("14", streak >= 14);

        return new Summary(
                checkedInToday,
                checkedInToday ? open.getCheckInAt() : null,
                streak,
                milestones,
                new Totals(thisWeek, thisMonth, allTime),
                occupancy);
    }

    /** All-time distinct attended days, computed in Mongo (groups by UTC day) so
     *  the full check-in history never leaves the database. */
    private int countDistinctDays(String userId) {
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("user").is(userId)),
                Aggregation.project()
                        .and(DateOperators.DateToString.dateOf("checkInAt").toString("%Y-%m-%d"))
                        .as("day"),
                Aggregation.group("day"));
        return mongo.aggregate(agg, Attendance.class, Document.class).getMappedResults().size();
    }

    public Trend getTrend(String userId, int days) {
        Instant since = TimeUtil.startOfDay().minus(days - 1L, ChronoUnit.DAYS);
        List<Attendance> records = attendance.findByUserAndCheckInAtGreaterThanEqual(userId, since);
        Set<String> present = new LinkedHashSet<>();
        for (Attendance r : records) {
            present.add(TimeUtil.dayKey(r.getCheckInAt()));
        }

        List<DayPoint> series = new ArrayList<>();
        LocalDate cursor = since.atZone(ZoneOffset.UTC).toLocalDate();
        for (int i = 0; i < days; i++) {
            String key = cursor.toString();
            series.add(new DayPoint(key, present.contains(key) ? 1 : 0));
            cursor = cursor.plusDays(1);
        }
        int attendedDays = (int) series.stream().filter(s -> s.present() == 1).count();
        int consistency = Math.round((attendedDays / (float) days) * 100);
        return new Trend(days, series, attendedDays, consistency);
    }

    public BestTime getBestTime() {
        Instant since = TimeUtil.daysAgo(30);
        List<Attendance> records = attendance.findByCheckInAtGreaterThanEqual(since);
        Map<Integer, Integer> byHour = new HashMap<>();
        for (Attendance r : records) {
            byHour.merge(TimeUtil.hourOf(r.getCheckInAt()), 1, Integer::sum);
        }
        List<HourCount> hours = new ArrayList<>();
        for (int h = 5; h <= 22; h++) {
            hours.add(new HourCount(h, byHour.getOrDefault(h, 0)));
        }
        List<HourCount> quietest = hours.stream()
                .sorted(Comparator.comparingInt(HourCount::count))
                .limit(3)
                .toList();
        Integer suggestion = quietest.isEmpty() ? null : quietest.get(0).hour();
        return new BestTime(hours, quietest, suggestion);
    }

    public List<MonthEntry> getMonth(String userId, int year, int month) {
        Instant from = LocalDate.of(year, month, 1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant to = LocalDate.of(year, month, 1).plusMonths(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        return attendance.findByUserAndCheckInAtBetweenOrderByCheckInAtAsc(userId, from, to).stream()
                .map(r -> new MonthEntry(r.getId(), r.getCheckInAt(), r.getCheckOutAt(), TimeUtil.dayKey(r.getCheckInAt())))
                .toList();
    }

    private static Instant dayStart(String dayKey) {
        return LocalDate.parse(dayKey).atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
