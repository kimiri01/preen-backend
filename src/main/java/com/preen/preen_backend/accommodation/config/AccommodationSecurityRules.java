package com.preen.preen_backend.accommodation.config;

import com.preen.preen_backend.common.security.SecurityRuleContributor;
import org.springframework.stereotype.Component;

/** 숙소·카테고리·지역 조회 API를 로그인 없이 열어 둡니다. */
@Component
public class AccommodationSecurityRules implements SecurityRuleContributor {

    @Override
    public String[] publicGetPaths() {
        return new String[] {"/api/accommodations/**", "/api/categories/**", "/api/regions/**"};
    }
}
