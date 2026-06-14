package com.yuvaraj.fitsphere.service;

import com.yuvaraj.fitsphere.domain.RefreshToken;
import com.yuvaraj.fitsphere.domain.Role;
import com.yuvaraj.fitsphere.domain.User;
import com.yuvaraj.fitsphere.dto.AuthDtos.AuthResponse;
import com.yuvaraj.fitsphere.dto.AuthDtos.PreferencesRequest;
import com.yuvaraj.fitsphere.dto.AuthDtos.PublicUser;
import com.yuvaraj.fitsphere.dto.AuthDtos.SigninRequest;
import com.yuvaraj.fitsphere.dto.AuthDtos.SignupRequest;
import com.yuvaraj.fitsphere.dto.AuthDtos.UpdateProfileRequest;
import com.yuvaraj.fitsphere.exception.HttpException;
import com.yuvaraj.fitsphere.repository.RefreshTokenRepository;
import com.yuvaraj.fitsphere.repository.UserRepository;
import com.yuvaraj.fitsphere.security.AppUser;
import com.yuvaraj.fitsphere.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuthService {

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserRepository users, RefreshTokenRepository refreshTokens,
                       PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    public AuthResponse signup(SignupRequest in) {
        if (users.findByEmail(in.email().toLowerCase()).isPresent()) {
            throw new HttpException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
        User u = new User();
        u.setName(in.name());
        u.setEmail(in.email().toLowerCase());
        u.setMobile(in.mobile());
        u.setPasswordHash(encoder.encode(in.password()));
        u.setRole(Role.MEMBER); // public signup is always a member
        users.save(u);
        return issue(u);
    }

    public AuthResponse signin(SigninRequest in) {
        User u = users.findByEmail(in.email().toLowerCase())
                .orElseThrow(() -> new HttpException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        if (!encoder.matches(in.password(), u.getPasswordHash())) {
            throw new HttpException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        return issue(u);
    }

    public AuthResponse refresh(String token) {
        refreshTokens.findByToken(token)
                .orElseThrow(() -> new HttpException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
        AppUser payload;
        try {
            payload = jwt.parseRefresh(token);
        } catch (Exception e) {
            refreshTokens.deleteByToken(token);
            throw new HttpException(HttpStatus.UNAUTHORIZED, "Expired refresh token");
        }
        User u = users.findById(payload.id())
                .orElseThrow(() -> new HttpException(HttpStatus.UNAUTHORIZED, "User no longer exists"));
        // Rotate: invalidate the old token, issue a fresh pair.
        refreshTokens.deleteByToken(token);
        return issue(u);
    }

    public void logout(String token) {
        refreshTokens.deleteByToken(token);
    }

    public PublicUser getMe(String userId) {
        return PublicUser.from(requireUser(userId));
    }

    public PublicUser updateProfile(String userId, UpdateProfileRequest in) {
        User u = requireUser(userId);
        if (in.name() != null) {
            u.setName(in.name());
        }
        if (in.mobile() != null) {
            u.setMobile(StringUtils.hasText(in.mobile()) ? in.mobile() : null);
        }
        users.save(u); // duplicate mobile -> DuplicateKeyException -> 409
        return PublicUser.from(u);
    }

    public void changePassword(String userId, String currentPassword, String newPassword) {
        User u = requireUser(userId);
        if (!encoder.matches(currentPassword, u.getPasswordHash())) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }
        u.setPasswordHash(encoder.encode(newPassword));
        users.save(u);
    }

    public PublicUser updatePreferences(String userId, PreferencesRequest prefs) {
        User u = requireUser(userId);
        if (prefs.theme() != null) {
            u.getPreferences().setTheme(prefs.theme());
        }
        if (prefs.density() != null) {
            u.getPreferences().setDensity(prefs.density());
        }
        if (prefs.locale() != null) {
            u.getPreferences().setLocale(prefs.locale());
        }
        users.save(u);
        return PublicUser.from(u);
    }

    private User requireUser(String userId) {
        return users.findById(userId)
                .orElseThrow(() -> new HttpException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private AuthResponse issue(User u) {
        String access = jwt.signAccess(u.getId(), u.getRole());
        String refresh = jwt.signRefresh(u.getId(), u.getRole());
        refreshTokens.save(new RefreshToken(refresh, u.getId(), jwt.refreshExpiry()));
        return new AuthResponse(PublicUser.from(u), access, refresh);
    }
}
