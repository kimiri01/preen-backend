package com.feature.auth.dto;

import com.feature.auth.entity.User;
import com.feature.common.security.UserRole;

// 가입 완료 후 회원 ID, 권한과 계정 상태를 반환합니다.
public record SignupResponse(Long userId, UserRole role, String status) {
    public static SignupResponse of(User user) {
        return new SignupResponse(user.getId(), user.getRole(), user.getStatus());
    }
}
