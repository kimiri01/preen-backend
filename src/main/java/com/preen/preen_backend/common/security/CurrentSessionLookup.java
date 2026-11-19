package com.preen.preen_backend.common.security;

// 인증 필터가 회원의 현재 세션인지 확인할 때 사용합니다.
public interface CurrentSessionLookup {
    boolean isCurrentSession(long userId, String sessionId);
}
