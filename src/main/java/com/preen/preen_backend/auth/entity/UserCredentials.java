package com.preen.preen_backend.auth.entity;

import jakarta.persistence.*;
import java.time.Duration;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 회원의 비밀번호 해시와 로그인 잠금 관련 정보를 저장합니다.
@Getter
@Entity
@Table(name = "user_credentials")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserCredentials {
    private static final int MAX_LOGIN_FAILURES = 5;
    private static final Duration LOGIN_LOCK_DURATION = Duration.ofMinutes(10);

    @Id @Column(name = "user_id")
    private Long userId;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "password_changed_at", nullable = false)
    private Instant passwordChangedAt;

    @Column(name = "failed_login_count", nullable = false)
    private short failedLoginCount;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    public boolean isLoginLocked(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    // 비밀번호가 틀리면 횟수를 늘리고, 다섯 번째 실패부터 10분간 잠급니다.
    public void recordLoginFailure(Instant now) {
        failedLoginCount++;
        if (failedLoginCount >= MAX_LOGIN_FAILURES) {
            lockedUntil = now.plus(LOGIN_LOCK_DURATION);
        }
    }

    // 로그인에 성공했거나 잠금 시간이 끝나면 이전 실패 기록을 지웁니다.
    public void clearLoginFailures() {
        this.failedLoginCount = 0;
        this.lockedUntil = null;
    }

    // 새 비밀번호의 해시와 변경 시각을 기록합니다.
    public void changePassword(String passwordHash, Instant changedAt) {
        this.passwordHash = passwordHash;
        this.passwordChangedAt = changedAt;
    }

    // 해시된 비밀번호로 신규 회원의 인증 정보를 생성합니다.
    public static UserCredentials create(Long userId, String passwordHash, Instant now) {
        return new UserCredentials(userId, passwordHash, now, (short) 0, null);
    }
}












