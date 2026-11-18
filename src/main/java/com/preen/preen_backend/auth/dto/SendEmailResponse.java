package com.preen.preen_backend.auth.dto;

import com.preen.preen_backend.auth.entity.VerificationCode;
import java.time.Instant;

// 인증 데이터 ID, 만료 시각과 재발송 대기 시간을 반환합니다.
public record SendEmailResponse(Long verificationId, Instant expiresAt, long resendAfterSeconds) {
    public static SendEmailResponse of(VerificationCode code, long resendAfterSeconds) {
        return new SendEmailResponse(code.getId(), code.getExpiresAt(), resendAfterSeconds);
    }
}
