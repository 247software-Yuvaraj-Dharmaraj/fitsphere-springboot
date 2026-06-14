package com.yuvaraj.fitsphere.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Setter
@Document(collection = "attendance")
// Hot path: summary/trend/month filter by user + checkInAt range. This compound
// index serves them in one shot; its `user` prefix also covers user-only lookups.
@CompoundIndex(name = "user_checkInAt_idx", def = "{'user': 1, 'checkInAt': 1}")
public class Attendance {

    @Id
    private String id;

    private String user;

    // Single index retained for the user-agnostic occupancy/best-time queries.
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
