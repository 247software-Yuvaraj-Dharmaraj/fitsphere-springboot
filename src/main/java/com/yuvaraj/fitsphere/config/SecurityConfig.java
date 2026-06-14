package com.yuvaraj.fitsphere.config;

import com.yuvaraj.fitsphere.security.JwtAuthFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;

@Configuration
public class SecurityConfig {

    private final String clientOrigin;

    public SecurityConfig(@Value("${app.client-origin}") String clientOrigin) {
        this.clientOrigin = clientOrigin;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/health").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/signup", "/api/auth/signin",
                                "/api/auth/refresh", "/api/auth/logout").permitAll()
                        // Analytics — staff only
                        .requestMatchers("/api/analytics/**").hasAnyRole("ADMIN", "TRAINER")
                        // Feedback — staff write/list; members read their own via /me
                        .requestMatchers(HttpMethod.POST, "/api/feedback").hasAnyRole("ADMIN", "TRAINER")
                        .requestMatchers(HttpMethod.GET, "/api/feedback/members").hasAnyRole("ADMIN", "TRAINER")
                        .requestMatchers(HttpMethod.GET, "/api/feedback/member/*").hasAnyRole("ADMIN", "TRAINER")
                        // Slot configuration — staff only (booking/waitlist endpoints stay member-accessible)
                        .requestMatchers(HttpMethod.POST, "/api/slots").hasAnyRole("ADMIN", "TRAINER")
                        .requestMatchers(HttpMethod.POST, "/api/slots/bulk-delete").hasAnyRole("ADMIN", "TRAINER")
                        .requestMatchers(HttpMethod.PATCH, "/api/slots/*").hasAnyRole("ADMIN", "TRAINER")
                        .requestMatchers(HttpMethod.DELETE, "/api/slots/*").hasAnyRole("ADMIN", "TRAINER")
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> writeError(res, HttpStatus.UNAUTHORIZED, "Authentication required"))
                        .accessDeniedHandler((req, res, e) -> writeError(res, HttpStatus.FORBIDDEN, "You do not have permission to perform this action")))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private static void writeError(jakarta.servlet.http.HttpServletResponse res, HttpStatus status, String message) throws IOException {
        res.setStatus(status.value());
        res.setContentType("application/json");
        res.getWriter().write("{\"message\":\"" + message + "\"}");
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(clientOrigin));
        config.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
