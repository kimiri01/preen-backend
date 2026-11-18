package com.preen.preen_backend.auth.config;

import com.preen.preen_backend.common.security.SecurityRuleContributor;
import org.springframework.stereotype.Component;

// 이메일 인증과 회원가입에 필요한 공개 API 경로를 제공합니다.
@Component
public class AuthSecurityRules implements SecurityRuleContributor {
    // 로그인 전에 호출하는 이메일 인증·회원가입 경로를 반환합니다.
    @Override
    public String[] publicPostPaths() {
        return new String[] {"/api/auth/email/send", "/api/auth/email/verify", "/api/auth/signup"};
    }
}
