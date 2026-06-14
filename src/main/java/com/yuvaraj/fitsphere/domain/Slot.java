package com.yuvaraj.fitsphere.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Document(collection = "slots")
public class Slot {

    @Id
    private String id;

    @Indexed
    private Instant date;

    private String startTime; // "06:00"

    private String endTime;   // "07:00"

    private int capacity;

    /** Confirmed bookings — user ids. */
    private List<String> bookings = new ArrayList<>();

    /** FIFO waitlist — user ids promoted into bookings as seats free up. */
    private List<String> waitlist = new ArrayList<>();
}
