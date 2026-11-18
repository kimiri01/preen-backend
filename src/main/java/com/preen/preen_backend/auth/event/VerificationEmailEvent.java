package com.preen.preen_backend.auth.event;

// 인증번호가 로그에 출력되지 않도록 Record 기본 toString을 사용하지 않습니다.
// 인증메일 발송에 필요한 이메일과 인증번호를 전달합니다.
public record VerificationEmailEvent(String email, String code) {
    @Override public String toString() { return "VerificationEmailEvent[redacted]"; }
}
