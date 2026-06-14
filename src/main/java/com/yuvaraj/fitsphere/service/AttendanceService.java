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

    // Gym-wide timezone for GLOBAL metrics (live-occupancy day boundary, busiest
    // hours) — minutes east of UTC. Personal stats use the caller's own offset.
    private static final int GYM_OFFSET = parseOffset(System.getenv("GYM_TZ_OFFSET"));

    private static int parseOffset(String v) {
        try {
            return v == null ? 0 : Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

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
        int activeCount = (int) attendance.countByCheckOutAtIsNullAndCheckInAtGreaterThanEqual(TimeUtil.startOfDay(GYM_OFFSET));
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
        Instant today = TimeUtil.startOfDay(GYM_OFFSET);
        attendance.findFirstByUserAndCheckOutAtIsNull(userId).ifPresent(open -> {
            if (!open.getCheckInAt().isBefore(today)) {
                throw new HttpException(HttpStatus.CONFLICT, "You are already checked in");
            }
            open.setCheckOutAt(today); // stale session from an earlier day — auto-close
            attendance.save(open);
        });

        // Occupancy is a count ACROSS documents, so capacity can't be guard-inserted
        // atomically. Fast-reject when already full, then create and re-check: if this
        // check-in pushed the gym over capacity, roll it back. Closes the read-then-
        // create race; any near-simultaneous overlap self-corrects as members leave.
        int cap = capacity();
        if (attendance.countByCheckOutAtIsNullAndCheckInAtGreaterThanEqual(today) >= cap) {
            throw new HttpException(HttpStatus.CONFLICT, "The gym is at full capacity. Please try again later.");
        }

        Attendance record = attendance.save(new Attendance(userId, Instant.now()));
        if (attendance.countByCheckOutAtIsNullAndCheckInAtGreaterThanEqual(today) > cap) {
            attendance.deleteById(record.getId()); // we tipped it over — undo
            throw new HttpException(HttpStatus.CONFLICT, "The gym is at full capacity. Please try again later.");
        }

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

    private int computeStreak(Set<String> dayKeys, int offsetMin) {
        int streak = 0;
        LocalDate cursor = LocalDate.now(TimeUtil.offset(offsetMin));
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

    public Summary getSummary(String userId, int offsetMin) {
        Attendance open = attendance.findFirstByUserAndCheckOutAtIsNull(userId).orElse(null);
        // Load only a bounded recent window (enough for streak + this week/month)
        // rather than the full history on every summary call.
        Instant windowStart = TimeUtil.startOfDay(offsetMin).minus(STREAK_WINDOW_DAYS, ChronoUnit.DAYS);
        List<Attendance> recent = attendance.findByUserAndCheckInAtGreaterThanEqual(userId, windowStart);
        Occupancy occupancy = getOccupancy();

        Set<String> dayKeys = new LinkedHashSet<>();
        for (Attendance a : recent) {
            dayKeys.add(TimeUtil.dayKey(a.getCheckInAt(), offsetMin));
        }
        int streak = computeStreak(dayKeys, offsetMin);

        Instant weekStart = TimeUtil.startOfWeek(offsetMin);
        Instant monthStart = TimeUtil.startOfMonth(offsetMin);
        int thisWeek = (int) dayKeys.stream().filter(k -> !TimeUtil.dayStart(k, offsetMin).isBefore(weekStart)).count();
        int thisMonth = (int) dayKeys.stream().filter(k -> !TimeUtil.dayStart(k, offsetMin).isBefore(monthStart)).count();
        int allTime = countDistinctDays(userId, offsetMin); // server-side, never pulls full history

        boolean checkedInToday = open != null && !open.getCheckInAt().isBefore(TimeUtil.startOfDay(offsetMin));
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
    private int countDistinctDays(String userId, int offsetMin) {
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("user").is(userId)),
                Aggregation.project()
                        .and(DateOperators.DateToString.dateOf("checkInAt").toString("%Y-%m-%d")
                                .withTimezone(DateOperators.Timezone.valueOf(TimeUtil.offsetId(offsetMin))))
                        .as("day"),
                Aggregation.group("day"));
        return mongo.aggregate(agg, Attendance.class, Document.class).getMappedResults().size();
    }

    public Trend getTrend(String userId, int days, int offsetMin) {
        Instant since = TimeUtil.startOfDay(offsetMin).minus(days - 1L, ChronoUnit.DAYS);
        List<Attendance> records = attendance.findByUserAndCheckInAtGreaterThanEqual(userId, since);
        Set<String> present = new LinkedHashSet<>();
        for (Attendance r : records) {
            present.add(TimeUtil.dayKey(r.getCheckInAt(), offsetMin));
        }

        List<DayPoint> series = new ArrayList<>();
        LocalDate cursor = since.atOffset(TimeUtil.offset(offsetMin)).toLocalDate();
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
        // Gym-wide busiest hours — grouped in the gym's timezone, not UTC.
        Instant since = TimeUtil.daysAgo(30, GYM_OFFSET);
        List<Attendance> records = attendance.findByCheckInAtGreaterThanEqual(since);
        Map<Integer, Integer> byHour = new HashMap<>();
        for (Attendance r : records) {
            byHour.merge(TimeUtil.hourOf(r.getCheckInAt(), GYM_OFFSET), 1, Integer::sum);
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

    public List<MonthEntry> getMonth(String userId, int year, int month, int offsetMin) {
        ZoneOffset z = TimeUtil.offset(offsetMin);
        Instant from = LocalDate.of(year, month, 1).atStartOfDay(z).toInstant();
        Instant to = LocalDate.of(year, month, 1).plusMonths(1).atStartOfDay(z).toInstant();
        return attendance.findByUserAndCheckInAtBetweenOrderByCheckInAtAsc(userId, from, to).stream()
                .map(r -> new MonthEntry(r.getId(), r.getCheckInAt(), r.getCheckOutAt(), TimeUtil.dayKey(r.getCheckInAt(), offsetMin)))
                .toList();
    }
}
