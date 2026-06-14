package com.yuvaraj.fitsphere.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class SlotDtos {

    private SlotDtos() {
    }

    private static final String TIME = "^([01]\\d|2[0-3]):[0-5]\\d$";
    private static final String DATE = "^\\d{4}-\\d{2}-\\d{2}$";

    public record CreateSlotRequest(
            @Pattern(regexp = DATE, message = "date must be YYYY-MM-DD") String date,
            @Pattern(regexp = TIME, message = "startTime must be HH:MM") String startTime,
            @Pattern(regexp = TIME, message = "endTime must be HH:MM") String endTime,
            @Min(1) @Max(500) int capacity) {
    }

    public record UpdateSlotRequest(
            @Pattern(regexp = TIME, message = "startTime must be HH:MM") String startTime,
            @Pattern(regexp = TIME, message = "endTime must be HH:MM") String endTime,
            @Min(1) @Max(500) int capacity) {
    }

    public record BulkDeleteRequest(@NotEmpty @Size(max = 100) List<String> ids) {
    }

    public record BulkDeleteResult(long deleted) {
    }

    public record SlotDto(
            String id, Instant date, String startTime, String endTime, int capacity,
            int bookedCount, int available, boolean bookedByMe, boolean isFull,
            int waitlistCount, boolean waitlistedByMe, Integer waitlistPosition) {
    }

    public record ListByDate(String date, List<SlotDto> slots) {
    }
}
