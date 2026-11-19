package com.preen.preen_backend.auth.dto;

import com.preen.preen_backend.common.util.EmailUtil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 50) String password,
        Boolean rememberMe) {

    public LoginRequest {
        if (email != null) {
            email = EmailUtil.normalize(email);
        }
    }

    @Override
    public String toString() {
        return "LoginRequest[redacted]";
    }
}
