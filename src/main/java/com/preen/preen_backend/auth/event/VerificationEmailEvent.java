package com.preen.preen_backend.auth.event;

// 인증메일 발송에 필요한 이메일과 인증번호를 전달합니다.
public record VerificationEmailEvent(String email, String code) {
    @Override public String toString() { return "VerificationEmailEvent[redacted]"; }
}
