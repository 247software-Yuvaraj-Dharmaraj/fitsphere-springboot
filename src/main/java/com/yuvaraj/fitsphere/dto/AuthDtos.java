package com.yuvaraj.fitsphere.dto;

import com.yuvaraj.fitsphere.domain.Role;
import com.yuvaraj.fitsphere.domain.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record SignupRequest(
            @NotBlank @Size(min = 2, message = "Name is too short") String name,
            @Email(message = "Invalid email") @NotBlank String email,
            @Size(min = 7, max = 15) String mobile,
            @NotBlank @Size(min = 6, message = "Password must be at least 6 characters") String password) {
    }

    public record SigninRequest(
            @Email(message = "Invalid email") @NotBlank String email,
            @NotBlank(message = "Password is required") String password) {
    }

    public record RefreshRequest(@NotBlank(message = "Refresh token is required") String refreshToken) {
    }

    public record UpdateProfileRequest(
            @Size(min = 2, message = "Name is too short") String name,
            @Size(min = 7, max = 15) String mobile) {
    }

    public record ChangePasswordRequest(
            @NotBlank(message = "Current password is required") String currentPassword,
            @NotBlank @Size(min = 6, message = "Password must be at least 6 characters") String newPassword) {
    }

    public record PreferencesRequest(String theme, String density, String locale) {
    }

    public record PreferencesDto(String theme, String density, String locale) {
        public static PreferencesDto from(User.Preferences p) {
            User.Preferences src = p != null ? p : new User.Preferences();
            return new PreferencesDto(src.getTheme(), src.getDensity(), src.getLocale());
        }
    }

    public record PublicUser(String id, String name, String email, String mobile, Role role, PreferencesDto preferences) {
        public static PublicUser from(User u) {
            return new PublicUser(u.getId(), u.getName(), u.getEmail(), u.getMobile(), u.getRole(),
                    PreferencesDto.from(u.getPreferences()));
        }
    }

    public record AuthResponse(PublicUser user, String accessToken, String refreshToken) {
    }
}
