package com.preen.preen_backend.common.auth.temporary;

/**
 * 현재 요청의 로그인 사용자 정보입니다.
 * 인증 기능이 붙으면 CurrentUserArgumentResolver의 구현만 바꾸면 됩니다.
 */
public record AuthUser(Long userId) {
}
