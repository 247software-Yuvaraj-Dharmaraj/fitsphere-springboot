package com.yuvaraj.fitsphere.web;

import com.yuvaraj.fitsphere.dto.AuthDtos.AuthResponse;
import com.yuvaraj.fitsphere.dto.AuthDtos.ChangePasswordRequest;
import com.yuvaraj.fitsphere.dto.AuthDtos.PreferencesRequest;
import com.yuvaraj.fitsphere.dto.AuthDtos.PublicUser;
import com.yuvaraj.fitsphere.dto.AuthDtos.RefreshRequest;
import com.yuvaraj.fitsphere.dto.AuthDtos.SigninRequest;
import com.yuvaraj.fitsphere.dto.AuthDtos.SignupRequest;
import com.yuvaraj.fitsphere.dto.AuthDtos.UpdateProfileRequest;
import com.yuvaraj.fitsphere.exception.HttpException;
import com.yuvaraj.fitsphere.security.AppUser;
import com.yuvaraj.fitsphere.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(auth.signup(req));
    }

    @PostMapping("/signin")
    public AuthResponse signin(@Valid @RequestBody SigninRequest req) {
        return auth.signin(req);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest req) {
        return auth.refresh(req.refreshToken());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest req) {
        auth.logout(req.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public PublicUser me(@AuthenticationPrincipal AppUser principal) {
        return auth.getMe(principal.id());
    }

    @PatchMapping("/me")
    public PublicUser updateProfile(@Valid @RequestBody UpdateProfileRequest req, @AuthenticationPrincipal AppUser principal) {
        if (req.name() == null && req.mobile() == null) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "Nothing to update");
        }
        return auth.updateProfile(principal.id(), req);
    }

    @PostMapping("/me/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest req, @AuthenticationPrincipal AppUser principal) {
        auth.changePassword(principal.id(), req.currentPassword(), req.newPassword());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me/preferences")
    public PublicUser updatePreferences(@Valid @RequestBody PreferencesRequest req, @AuthenticationPrincipal AppUser principal) {
        if (req.theme() == null && req.density() == null && req.locale() == null) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "At least one preference is required");
        }
        return auth.updatePreferences(principal.id(), req);
    }
}
