package com.feature.auth.controller;

import lombok.RequiredArgsConstructor;
import com.feature.auth.dto.*;
import com.feature.auth.service.EmailVerificationService;
import com.feature.common.util.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import com.feature.common.response.ApiResponse;
import org.springframework.web.bind.annotation.*;

// 회원가입 이메일 인증번호 발송·검증 API를 제공합니다.
@RestController
@RequestMapping("/api/auth/email")
@RequiredArgsConstructor
public class EmailVerificationController {
    private final EmailVerificationService service;
    private final RateLimiter rateLimiter;

    // POST /api/auth/email/send — 로그인 없이 인증번호 발송을 요청합니다.
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<SendEmailResponse>> send(
            @Valid
            @RequestBody
            SendEmailRequest request,
            HttpServletRequest httpRequest) {
        rateLimiter.check("email-send:" + httpRequest.getRemoteAddr(), 10);
        return ResponseEntity.ok(ApiResponse.success(service.send(request)));
    }

    // POST /api/auth/email/verify — 로그인 없이 인증번호를 검증하고 가입용 토큰을 받습니다.
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<VerifyEmailResponse>> verify(
            @Valid
            @RequestBody
            VerifyEmailRequest request,
            HttpServletRequest httpRequest) {
        rateLimiter.check("email-verify:" + httpRequest.getRemoteAddr(), 30);
        return ResponseEntity.ok(ApiResponse.success(service.verify(request)));
    }
}
