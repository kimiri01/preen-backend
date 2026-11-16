package com.feature.common.security;

// 각 기능에서 로그인 없이 허용할 POST 경로를 제공합니다.
public interface SecurityRuleContributor {
    String[] publicPostPaths();
}
