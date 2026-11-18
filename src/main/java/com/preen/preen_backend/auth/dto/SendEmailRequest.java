package com.preen.preen_backend.auth.dto;

import com.preen.preen_backend.common.util.EmailUtil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 인증번호를 받을 이메일 주소를 전달합니다.
public record SendEmailRequest(@NotBlank @Email @Size(max = 254) String email) {
    public SendEmailRequest { if (email != null) email = EmailUtil.normalize(email); }
}
