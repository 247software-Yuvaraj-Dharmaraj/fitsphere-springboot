package com.yuvaraj.fitsphere.dto;

import com.yuvaraj.fitsphere.domain.WorkoutType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Map;

public final class WorkoutDtos {

    private WorkoutDtos() {
    }

    public record LogWorkoutRequest(
            @NotNull WorkoutType type,
            @Min(1) @Max(600) int durationMin,
            String date) {
    }

    public record WorkoutDto(String id, WorkoutType type, int durationMin, Instant date) {
    }

    public record Stats(Map<String, Integer> byType, int totalSessions, int totalMinutes,
                        int thisWeekSessions, int avgDuration) {
    }
}
