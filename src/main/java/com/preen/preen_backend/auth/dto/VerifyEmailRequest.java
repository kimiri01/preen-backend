package com.preen.preen_backend.auth.dto;

import com.preen.preen_backend.common.util.EmailUtil;
import jakarta.validation.constraints.*;

// 이메일, 인증 요청 ID, 사용자가 입력한 인증번호를 받는 DTO
public record VerifyEmailRequest(
        @NotBlank @Email @Size(max = 254) String email,

        @NotNull Long verificationId,

        @NotBlank @Pattern(regexp = "[0-9]{6}") String code
) {
    public VerifyEmailRequest {
        if (email != null) email = EmailUtil.normalize(email);
    }

    @Override
    public String toString() {
        return "VerifyEmailRequest[redacted]";
    }
}