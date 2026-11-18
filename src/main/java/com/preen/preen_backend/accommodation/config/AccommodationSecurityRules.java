package com.preen.preen_backend.accommodation.config;

import com.preen.preen_backend.common.security.SecurityRuleContributor;
import org.springframework.stereotype.Component;

/**
 * 숙소·카테고리·지역 조회 API는 로그인하지 않은 사용자도 볼 수 있어야 하므로 공개합니다.
 * (메인 페이지, 카테고리 바, 최근 본 숙소 등)
 */
@Component
public class AccommodationSecurityRules implements SecurityRuleContributor {

    @Override
    public String[] publicGetPaths() {
        return new String[] {"/api/accommodations/**", "/api/categories/**", "/api/regions/**"};
    }
}
