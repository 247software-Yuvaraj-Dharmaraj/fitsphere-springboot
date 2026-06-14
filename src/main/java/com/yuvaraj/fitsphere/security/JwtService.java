package com.yuvaraj.fitsphere.security;

import com.yuvaraj.fitsphere.domain.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey accessKey;
    private final SecretKey refreshKey;
    private final long accessTtlMinutes;
    private final long refreshTtlDays;

    public JwtService(@Value("${app.jwt.access-secret}") String accessSecret,
                      @Value("${app.jwt.refresh-secret}") String refreshSecret,
                      @Value("${app.jwt.access-ttl-minutes}") long accessTtlMinutes,
                      @Value("${app.jwt.refresh-ttl-days}") long refreshTtlDays) {
        this.accessKey = Keys.hmacShaKeyFor(sha256(accessSecret));
        this.refreshKey = Keys.hmacShaKeyFor(sha256(refreshSecret));
        this.accessTtlMinutes = accessTtlMinutes;
        this.refreshTtlDays = refreshTtlDays;
    }

    public String signAccess(String userId, Role role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId)
                .claim("role", role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(Duration.ofMinutes(accessTtlMinutes))))
                .signWith(accessKey)
                .compact();
    }

    public String signRefresh(String userId, Role role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId)
                .claim("role", role.name())
                .id(UUID.randomUUID().toString()) // jti — unique per token, so rotation always differs
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(Duration.ofDays(refreshTtlDays))))
                .signWith(refreshKey)
                .compact();
    }

    public AppUser parseAccess(String token) {
        return parse(token, accessKey);
    }

    public AppUser parseRefresh(String token) {
        return parse(token, refreshKey);
    }

    public Instant refreshExpiry() {
        return Instant.now().plus(Duration.ofDays(refreshTtlDays));
    }

    private AppUser parse(String token, SecretKey key) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        return new AppUser(claims.getSubject(), Role.valueOf(claims.get("role", String.class)));
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
