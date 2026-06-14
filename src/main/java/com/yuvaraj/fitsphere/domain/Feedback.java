package com.yuvaraj.fitsphere.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Setter
@Document(collection = "feedback")
public class Feedback {

    @Id
    private String id;

    private String trainer;

    @Indexed
    private String member;

    private String note;

    private Instant weekOf;

    @CreatedDate
    private Instant createdAt;
}
