package com.preen.preen_backend.common.security;

// 보호된 API에서 사용할 로그인 회원 정보입니다.
public record AuthenticatedUser(long userId, UserRole role, String sessionId) {
}
