package com.yuvaraj.fitsphere.service;

import com.yuvaraj.fitsphere.domain.WorkoutLog;
import com.yuvaraj.fitsphere.domain.WorkoutType;
import com.yuvaraj.fitsphere.dto.WorkoutDtos.LogWorkoutRequest;
import com.yuvaraj.fitsphere.dto.WorkoutDtos.Stats;
import com.yuvaraj.fitsphere.dto.WorkoutDtos.WorkoutDto;
import com.yuvaraj.fitsphere.repository.WorkoutLogRepository;
import com.yuvaraj.fitsphere.util.TimeUtil;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class WorkoutService {

    private final WorkoutLogRepository workouts;

    public WorkoutService(WorkoutLogRepository workouts) {
        this.workouts = workouts;
    }

    public WorkoutDto log(String userId, LogWorkoutRequest in) {
        Instant date = in.date() != null ? parseInstant(in.date()) : Instant.now();
        WorkoutLog saved = workouts.save(new WorkoutLog(userId, in.type(), in.durationMin(), date));
        return toDto(saved);
    }

    public List<WorkoutDto> recent(String userId, int limit) {
        return workouts.findByUserOrderByDateDesc(userId, PageRequest.of(0, limit)).stream()
                .map(this::toDto)
                .toList();
    }

    public Stats stats(String userId) {
        List<WorkoutLog> logs = workouts.findByUser(userId);
        Map<String, Integer> byType = new LinkedHashMap<>();
        for (WorkoutType t : WorkoutType.values()) {
            byType.put(t.name(), 0);
        }
        int totalMinutes = 0;
        int thisWeekSessions = 0;
        Instant weekStart = TimeUtil.startOfWeek();
        for (WorkoutLog l : logs) {
            byType.merge(l.getType().name(), 1, Integer::sum);
            totalMinutes += l.getDurationMin();
            if (!l.getDate().isBefore(weekStart)) {
                thisWeekSessions++;
            }
        }
        int totalSessions = logs.size();
        int avg = totalSessions > 0 ? Math.round(totalMinutes / (float) totalSessions) : 0;
        return new Stats(byType, totalSessions, totalMinutes, thisWeekSessions, avg);
    }

    private WorkoutDto toDto(WorkoutLog l) {
        return new WorkoutDto(l.getId(), l.getType(), l.getDurationMin(), l.getDate());
    }

    private static Instant parseInstant(String value) {
        try {
            return Instant.parse(value);
        } catch (Exception ignored) {
            return OffsetDateTime.parse(value).toInstant();
        }
    }
}
