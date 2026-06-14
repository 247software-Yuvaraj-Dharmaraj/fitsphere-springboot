package com.yuvaraj.fitsphere.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class FeedbackDtos {

    private FeedbackDtos() {
    }

    public record CreateFeedbackRequest(
            @NotBlank(message = "memberId is required") String memberId,
            @NotBlank(message = "Feedback note is required") @Size(max = 1000) String note,
            String weekOf) {
    }

    public record FeedbackDto(String id, String note, Instant weekOf, Instant createdAt, String trainerName) {
    }

    public record MemberDto(String id, String name, String email) {
    }
}
