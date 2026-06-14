package com.yuvaraj.fitsphere.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document(collection = "gym_config")
public class GymConfig {

    @Id
    private String id;

    private String name = "FitSphere Gym";

    private int capacity = 50;
}
