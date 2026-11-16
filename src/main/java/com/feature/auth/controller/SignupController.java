package com.feature.auth.controller;

import com.feature.auth.dto.SignupRequest;
import com.feature.auth.dto.SignupResponse;
import com.feature.auth.service.SignupService;
import com.feature.common.util.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.feature.common.response.ApiResponse;
import org.springframework.web.bind.annotation.*;

// 이메일 인증을 마친 사용자의 회원가입 API를 제공합니다.
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class SignupController {
    private final SignupService signupService;
    private final RateLimiter rateLimiter;

    // POST /api/auth/signup — 로그인 없이 인증 증명과 가입 정보를 전달해 회원을 등록합니다.
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(
            @Valid
            @RequestBody
            SignupRequest request,
            HttpServletRequest httpRequest) {
        rateLimiter.check("signup:" + httpRequest.getRemoteAddr(), 10);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(signupService.signup(request)));
    }
}
