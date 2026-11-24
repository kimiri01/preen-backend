package com.preen.preen_backend.auth.dto;

import com.preen.preen_backend.common.security.UserRole;

public record CurrentMemberResponse(long userId, String name, UserRole role) {
}
