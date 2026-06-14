package com.yuvaraj.fitsphere.service;

import com.yuvaraj.fitsphere.domain.Attendance;
import com.yuvaraj.fitsphere.domain.Role;
import com.yuvaraj.fitsphere.domain.User;
import com.yuvaraj.fitsphere.dto.AnalyticsDtos.DayCount;
import com.yuvaraj.fitsphere.dto.AnalyticsDtos.MemberRow;
import com.yuvaraj.fitsphere.dto.AnalyticsDtos.MembersResponse;
import com.yuvaraj.fitsphere.dto.AnalyticsDtos.Overview;
import com.yuvaraj.fitsphere.dto.AnalyticsDtos.Totals;
import com.yuvaraj.fitsphere.dto.AttendanceDtos.HourCount;
import com.yuvaraj.fitsphere.repository.AttendanceRepository;
import com.yuvaraj.fitsphere.repository.UserRepository;
import com.yuvaraj.fitsphere.util.TimeUtil;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final AttendanceRepository attendance;
    private final UserRepository users;
    private final AttendanceService attendanceService;

    public AnalyticsService(AttendanceRepository attendance, UserRepository users, AttendanceService attendanceService) {
        this.attendance = attendance;
        this.users = users;
        this.attendanceService = attendanceService;
    }

    public Overview overview() {
        Instant since30 = TimeUtil.daysAgo(30);
        Instant since14 = TimeUtil.daysAgo(13);
        Instant weekStart = TimeUtil.startOfWeek();

        List<Attendance> last30 = attendance.findByCheckInAtGreaterThanEqual(since30);

        // Peak hours 0-23.
        int[] hourCounts = new int[24];
        for (Attendance a : last30) {
            hourCounts[TimeUtil.hourOf(a.getCheckInAt())]++;
        }
        List<HourCount> peakHours = new ArrayList<>();
        int busiestHour = 0;
        for (int h = 0; h < 24; h++) {
            peakHours.add(new HourCount(h, hourCounts[h]));
            if (hourCounts[h] > hourCounts[busiestHour]) busiestHour = h;
        }
        Integer peakHour = hourCounts[busiestHour] > 0 ? busiestHour : null;

        // Daily trend over 14 days.
        Map<String, Integer> dayMap = new HashMap<>();
        for (Attendance a : last30) {
            if (!a.getCheckInAt().isBefore(since14)) {
                dayMap.merge(TimeUtil.dayKey(a.getCheckInAt()), 1, Integer::sum);
            }
        }
        List<DayCount> dailyTrend = new ArrayList<>();
        LocalDate cursor = since14.atZone(ZoneOffset.UTC).toLocalDate();
        for (int i = 0; i < 14; i++) {
            String key = cursor.toString();
            dailyTrend.add(new DayCount(key, dayMap.getOrDefault(key, 0)));
            cursor = cursor.plusDays(1);
        }

        long totalMembers = users.countByRole(Role.MEMBER);
        long activeThisWeek = attendance.findByCheckInAtGreaterThanEqual(weekStart).stream()
                .map(Attendance::getUser).distinct().count();

        return new Overview(
                new Totals(totalMembers, (int) activeThisWeek, peakHour),
                attendanceService.getOccupancy(),
                peakHours,
                dailyTrend);
    }

    public MembersResponse members(String q, int page, int pageSize, String sort, String dir, String status) {
        Instant weekStart = TimeUtil.startOfWeek();
        Instant now = Instant.now();

        List<User> members = users.findByRoleOrderByNameAsc(Role.MEMBER);
        if (q != null && !q.isBlank()) {
            Pattern rx = Pattern.compile(Pattern.quote(q.trim()), Pattern.CASE_INSENSITIVE);
            members = members.stream()
                    .filter(m -> rx.matcher(m.getName()).find() || rx.matcher(m.getEmail()).find())
                    .toList();
        }

        // Group attendance by user once.
        Map<String, List<Attendance>> byUser = attendance.findAll().stream()
                .collect(Collectors.groupingBy(Attendance::getUser));

        List<Row> rows = new ArrayList<>();
        for (User m : members) {
            List<Attendance> att = byUser.getOrDefault(m.getId(), List.of());
            int totalVisits = att.size();
            int thisWeek = (int) att.stream().filter(a -> !a.getCheckInAt().isBefore(weekStart)).count();
            Instant lastVisit = att.stream().map(Attendance::getCheckInAt).max(Comparator.naturalOrder()).orElse(null);
            double ageDays = lastVisit == null ? 99999 : (now.toEpochMilli() - lastVisit.toEpochMilli()) / 86_400_000.0;
            String st = ageDays <= 7 ? "ACTIVE" : ageDays <= 14 ? "AT_RISK" : "INACTIVE";
            int rank = ageDays <= 7 ? 0 : ageDays <= 14 ? 1 : 2;
            rows.add(new Row(m.getId(), m.getName(), m.getEmail(), totalVisits, thisWeek, lastVisit, st, rank));
        }

        if (!"ALL".equals(status)) {
            rows = rows.stream().filter(r -> r.status.equals(status)).toList();
        }

        Comparator<Row> cmp = switch (sort) {
            case "name" -> Comparator.comparing(r -> r.name, String.CASE_INSENSITIVE_ORDER);
            case "thisWeek" -> Comparator.comparingInt(r -> r.thisWeek);
            case "lastVisit" -> Comparator.comparing((Row r) -> r.lastVisit, Comparator.nullsFirst(Comparator.naturalOrder()));
            case "status" -> Comparator.comparingInt(r -> r.statusRank);
            default -> Comparator.comparingInt(r -> r.totalVisits); // totalVisits
        };
        if ("desc".equals(dir)) {
            cmp = cmp.reversed();
        }
        cmp = cmp.thenComparing(r -> r.id);

        long total = rows.size();
        List<MemberRow> pageRows = rows.stream()
                .sorted(cmp)
                .skip((long) page * pageSize)
                .limit(pageSize)
                .map(r -> new MemberRow(r.id, r.name, r.email, r.totalVisits, r.thisWeek, r.lastVisit, r.status))
                .toList();

        return new MembersResponse(pageRows, total, page, pageSize);
    }

    private record Row(String id, String name, String email, int totalVisits, int thisWeek,
                       Instant lastVisit, String status, int statusRank) {
    }
}
