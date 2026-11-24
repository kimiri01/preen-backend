package com.preen.preen_backend.auth.dto;

// 응답 본문에는 Access Token만 담습니다.
public record AccessTokenResponse(String accessToken) {
    @Override
    public String toString() {
        return "AccessTokenResponse[redacted]";
    }
}
