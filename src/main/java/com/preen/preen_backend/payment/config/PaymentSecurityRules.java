package com.preen.preen_backend.payment.config;

import com.preen.preen_backend.common.security.SecurityRuleContributor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 로컬 개발 환경에서만 테스트 결제 API를 공개합니다.
 * 운영 환경에서는 활성화하지 않습니다.
 */
@Component
@Profile("local")
public class PaymentSecurityRules implements SecurityRuleContributor {

    @Override
    public String[] publicPostPaths() {
        return new String[] {
                "/api/payments/test",
                "/api/payments/*/complete"
        };
    }

    @Override
    public String[] publicGetPaths() {
        return new String[] {
                "/api/payments/*"
        };
    }
}