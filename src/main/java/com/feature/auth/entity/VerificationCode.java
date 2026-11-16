package com.feature.auth.entity;

import com.feature.common.entity.BaseCreatedEntity;
import jakarta.persistence.*;
import java.time.Instant;

// 이메일 인증번호 해시, 시도 횟수와 가입 인증 증명을 저장합니다.
@Entity
@Table(name = "verification_codes")
public class VerificationCode extends BaseCreatedEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "target_type", nullable = false, length = 20)
    private String targetType;

    @Column(name = "target_hash", nullable = false, columnDefinition = "char(64)")
    private String targetHash;

    @Column(nullable = false, length = 20)
    private String purpose;

    @Column(name = "code_hash", nullable = false, columnDefinition = "char(64)")
    private String codeHash;

    @Column(name = "attempt_count", nullable = false)
    private short attemptCount;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "signup_token_hash", columnDefinition = "char(64)")
    private String signupTokenHash;

    @Column(name = "signup_token_expires_at")
    private Instant signupTokenExpiresAt;

    protected VerificationCode() {}

    // 새 인증번호를 반영하고 이전 검증 상태와 가입 증명을 초기화합니다.
    public void issue(String hash, Instant expiry) {
        codeHash = hash;
        expiresAt = expiry;
        attemptCount = 0;
        verifiedAt = null;
        signupTokenHash = null;
        signupTokenExpiresAt = null;
    }
    public void fail() { attemptCount++; }
    // 인증 성공 시각과 가입용 토큰 해시·만료 시각을 기록합니다.
    public void verify(String tokenHash, Instant now, Instant expiry) {
        signupTokenHash = tokenHash;
        verifiedAt = now;
        signupTokenExpiresAt = expiry;
    }
    // 유효 기간이 남은 가입 인증 증명이 있는지 확인합니다.
    public boolean hasSignupProof(Instant now) {
        return verifiedAt != null && signupTokenHash != null && signupTokenExpiresAt != null
                && signupTokenExpiresAt.isAfter(now);
    }
    // 가입에 사용한 인증 증명을 제거해 재사용을 막습니다.
    public void consumeSignupProof() {
        signupTokenHash = null;
        signupTokenExpiresAt = null;
    }
    public Long getId() { return id; }
    public String getSignupTokenHash() { return signupTokenHash; }
    public String getCodeHash() { return codeHash; }
    public short getAttemptCount() { return attemptCount; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getVerifiedAt() { return verifiedAt; }
}
