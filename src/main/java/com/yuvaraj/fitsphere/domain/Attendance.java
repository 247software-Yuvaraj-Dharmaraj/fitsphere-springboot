package com.yuvaraj.fitsphere.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Setter
@Document(collection = "attendance")
public class Attendance {

    @Id
    private String id;

    @Indexed
    private String user;

    @Indexed
    private Instant checkInAt = Instant.now();

    private Instant checkOutAt;

    public Attendance() {
    }

    public Attendance(String user, Instant checkInAt) {
        this.user = user;
        this.checkInAt = checkInAt;
    }
}
