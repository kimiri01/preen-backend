package com.preen.preen_backend.auth.dto;

import java.time.Instant;

// 이메일 인증 완료 후 가입에 사용할 토큰과 만료 시각을 반환합니다.
public record VerifyEmailResponse(String signupToken, Instant expiresAt) {
    @Override public String toString() { return "VerifyEmailResponse[redacted]"; }
}
