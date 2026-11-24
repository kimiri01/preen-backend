package com.preen.preen_backend.common.security;

/** 기능별로 로그인 없이 허용할 API 경로를 SecurityConfig에 알려 주는 인터페이스 */
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
