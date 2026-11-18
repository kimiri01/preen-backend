package com.preen.preen_backend.auth.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

// 회원의 비밀번호 해시와 로그인 잠금 관련 정보를 저장합니다.
@Getter
@Entity
@Table(name = "user_credentials")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCredentials {
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

    // 해시된 비밀번호로 신규 회원의 인증 정보를 생성합니다.
    public static UserCredentials create(Long userId, String passwordHash, Instant now) {
        UserCredentials credentials = new UserCredentials();
        credentials.userId = userId;
        credentials.passwordHash = passwordHash;
        credentials.passwordChangedAt = now;
        return credentials;
    }
}
