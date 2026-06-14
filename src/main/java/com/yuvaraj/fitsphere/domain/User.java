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
@Document(collection = "users")
public class User {

    @Id
    private String id;

    private String name;

    @Indexed(unique = true)
    private String email;

    @Indexed(unique = true, sparse = true)
    private String mobile;

    private String passwordHash;

    private Role role = Role.MEMBER;

    private Preferences preferences = new Preferences();

    @CreatedDate
    private Instant createdAt;

    @Getter
    @Setter
    public static class Preferences {
        private String theme = "light";       // light | dark
        private String density = "comfortable"; // comfortable | compact
        private String locale = "en";
    }
}
