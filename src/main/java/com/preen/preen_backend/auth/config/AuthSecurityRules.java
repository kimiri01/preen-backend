package com.preen.preen_backend.auth.config;

import com.preen.preen_backend.common.security.SecurityRuleContributor;
import org.springframework.stereotype.Component;

// Access Token 없이 호출할 수 있는 인증 API 경로를 제공합니다.
@Component
public class AuthSecurityRules implements SecurityRuleContributor {
    // 로그인 없이 호출할 수 있는 인증 API 경로를 반환합니다.
    @Override
    public String[] publicPostPaths() {
        return new String[] {
            "/api/auth/email/send", "/api/auth/email/verify", "/api/auth/signup",
            "/api/auth/login", "/api/auth/refresh",
            "/api/auth/password-reset/request", "/api/auth/password-reset/confirm"
        };
    }
}
