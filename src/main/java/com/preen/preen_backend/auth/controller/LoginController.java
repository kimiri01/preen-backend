package com.preen.preen_backend.auth.controller;

import com.preen.preen_backend.auth.config.RefreshTokenCookie;
import com.preen.preen_backend.auth.dto.AccessTokenResponse;
import com.preen.preen_backend.auth.dto.CurrentMemberResponse;
import com.preen.preen_backend.auth.dto.LoginRequest;
import com.preen.preen_backend.auth.exception.AuthErrorCode;
import com.preen.preen_backend.auth.service.LoginSessionService;
import com.preen.preen_backend.auth.service.LoginSessionService.SessionTokens;
import com.preen.preen_backend.common.exception.BusinessException;
import com.preen.preen_backend.common.response.ApiResponse;
import com.preen.preen_backend.common.security.AuthenticatedUser;
import com.preen.preen_backend.common.security.SecurityProperties;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class LoginController {
    private final LoginSessionService sessions;
    private final RefreshTokenCookie cookies;
    private final SecurityProperties security;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AccessTokenResponse>> login(
            @RequestHeader(value = HttpHeaders.ORIGIN, required = false) String origin,
            @Valid @RequestBody LoginRequest request) {
        verifyOrigin(origin);
        return tokenResponse(sessions.login(request.email(), request.password(),
                !Boolean.FALSE.equals(request.rememberMe())));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CurrentMemberResponse>> me(
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(ApiResponse.ok(sessions.currentMember(principal)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AccessTokenResponse>> refresh(
            @RequestHeader(value = HttpHeaders.ORIGIN, required = false) String origin,
            @CookieValue(value = RefreshTokenCookie.NAME, required = false) String refreshToken) {
        verifyOrigin(origin);
        return tokenResponse(sessions.refresh(refreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = HttpHeaders.ORIGIN, required = false) String origin,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        verifyOrigin(origin);
        sessions.logout(principal);
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.SET_COOKIE, cookies.clear().toString()).build();
    }

    // 쿠키를 발급하거나 사용하는 요청은 허용된 웹페이지에서 보낸 요청만 처리합니다.
    private void verifyOrigin(String origin) {
        if (origin == null || !security.allowedOrigins().contains(origin)) {
            throw new BusinessException(AuthErrorCode.ORIGIN_NOT_ALLOWED);
        }
    }

    private ResponseEntity<ApiResponse<AccessTokenResponse>> tokenResponse(SessionTokens tokens) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.SET_COOKIE, cookies.create(tokens.refreshToken(), tokens.expiresAt(), tokens.rememberMe()).toString())
                .body(ApiResponse.ok(new AccessTokenResponse(tokens.accessToken())));
    }
}
