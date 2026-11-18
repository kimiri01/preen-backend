package com.preen.preen_backend.common.security;

/**
 * 각 기능(feature)이 "로그인 없이 호출해도 되는 API 경로"를 SecurityConfig에 알려 주는 약속입니다.
 * 새 기능을 만들 때 이 인터페이스를 구현한 @Component를 하나 만들면, SecurityConfig를 고치지 않아도 됩니다.
 */
public interface SecurityRuleContributor {

    /** 로그인 없이 허용할 POST 경로 (예: 회원가입) */
    default String[] publicPostPaths() {
        return new String[0];
    }

    /** 로그인 없이 허용할 GET 경로 (예: 숙소 목록 조회) */
    default String[] publicGetPaths() {
        return new String[0];
    }
}
