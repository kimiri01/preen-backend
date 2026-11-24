package com.preen.preen_backend.auth.dto;

import com.preen.preen_backend.common.util.EmailUtil;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

// 가입 인증 증명, 회원 정보와 약관 동의 내용을 전달합니다.
public record SignupRequest(

        @NotBlank @Email @Size(max = 254) String email,

        @NotBlank @Size(max = 100) String signupToken,

        @NotBlank @Size(max = 100) String name,

        @NotNull @Past LocalDate birthDate,

        @NotBlank @Size(min = 8, max = 50) String password,

        @NotBlank @Size(max = 50) String passwordConfirmation,

        @NotNull @AssertTrue Boolean termsAgreed,

        @NotNull @AssertTrue Boolean privacyAgreed,

        @NotNull Boolean marketingAgreed,

        @NotBlank @Size(max = 50) String termsVersion) {

    public SignupRequest {
        if (email != null) email = EmailUtil.normalize(email);
        if (name != null) name = name.strip();

    }

    @Override public String toString() { return "SignupRequest[redacted]"; }
}
