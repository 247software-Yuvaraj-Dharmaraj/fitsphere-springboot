package com.yuvaraj.fitsphere.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Setter
@Document(collection = "refresh_tokens")
public class RefreshToken {

    @Id
    private String id;

    @Indexed(unique = true)
    private String token;

    @Indexed
    private String user;

    /** TTL index — Mongo auto-deletes the document once expiresAt passes. */
    @Indexed(expireAfter = "0s")
    private Instant expiresAt;

    public RefreshToken() {
    }

    public RefreshToken(String token, String user, Instant expiresAt) {
        this.token = token;
        this.user = user;
        this.expiresAt = expiresAt;
    }
}
