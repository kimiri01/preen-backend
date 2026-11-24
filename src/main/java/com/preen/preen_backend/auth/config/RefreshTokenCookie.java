package com.preen.preen_backend.auth.config;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenCookie {
    public static final String NAME = "refresh_token";
    private static final String PATH = "/api/auth";

    private final Clock clock;
    private final boolean secure;

    public RefreshTokenCookie(Clock clock, @Value("${app.auth.refresh-cookie-secure}") boolean secure) {
        this.clock = clock;
        this.secure = secure;
    }

    // 재발급해도 처음 로그인에서 정한 만료 시각까지만 쿠키를 유지합니다.
    public ResponseCookie create(String token, Instant expiresAt, boolean rememberMe) {
        if (!rememberMe) return cookie(token, -1);
        long remaining = Math.max(0, Duration.between(clock.instant(), expiresAt).toSeconds());
        return cookie(token, remaining);
    }

    public ResponseCookie clear() {
        return cookie("", 0);
    }

    private ResponseCookie cookie(String value, long maxAge) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path(PATH)
                .maxAge(maxAge)
                .build();
    }
}
