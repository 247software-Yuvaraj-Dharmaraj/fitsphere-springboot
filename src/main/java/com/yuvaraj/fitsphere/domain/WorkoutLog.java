package com.yuvaraj.fitsphere.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Setter
@Document(collection = "workout_logs")
public class WorkoutLog {

    @Id
    private String id;

    @Indexed
    private String user;

    private WorkoutType type;

    private int durationMin;

    private Instant date = Instant.now();

    public WorkoutLog() {
    }

    public WorkoutLog(String user, WorkoutType type, int durationMin, Instant date) {
        this.user = user;
        this.type = type;
        this.durationMin = durationMin;
        this.date = date;
    }
}
