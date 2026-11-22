package com.preen.preen_backend.auth.controller;

import com.preen.preen_backend.auth.dto.PasswordResetConfirmRequest;
import com.preen.preen_backend.auth.dto.PasswordResetLinkRequest;
import com.preen.preen_backend.auth.service.PasswordResetService;
import com.preen.preen_backend.common.response.ApiResponse;
import com.preen.preen_backend.common.util.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/password-reset")
@RequiredArgsConstructor
public class PasswordResetController {
    private static final int REQUESTS_PER_IP = 5;

    private final PasswordResetService passwordResetService;
    private final RateLimiter rateLimiter;

    @PostMapping("/request")
    public ApiResponse<Void> requestLink(
            @Valid @RequestBody PasswordResetLinkRequest request, HttpServletRequest httpRequest) {
        // 같은 IP에서 여러 이메일로 계속 요청하는 것을 제한합니다.
        rateLimiter.check("password-reset-ip:" + httpRequest.getRemoteAddr(), REQUESTS_PER_IP);
        passwordResetService.requestLink(request.email());
        // 이메일의 가입 여부와 관계없이 같은 응답을 보냅니다.
        return ApiResponse.ok(null);
    }

    @PostMapping("/confirm")
    public ApiResponse<Void> confirm(@RequestBody PasswordResetConfirmRequest request) {
        passwordResetService.changePassword(request.token(), request.newPassword());
        return ApiResponse.ok(null);
    }
}
