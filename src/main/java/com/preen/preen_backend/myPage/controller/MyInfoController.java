package com.preen.preen_backend.myPage.controller;

import com.preen.preen_backend.common.response.ApiResponse;
import com.preen.preen_backend.myPage.dto.MyInfoResponse;
import com.preen.preen_backend.myPage.dto.MyInfoUpdateRequest;
import com.preen.preen_backend.myPage.service.MyInfoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/my/info")
public class MyInfoController {

    private final MyInfoService myInfoService;

    // 내 정보 조회
    @GetMapping
    public ResponseEntity<ApiResponse<MyInfoResponse>> getMyInfo(
            @AuthenticationPrincipal(expression = "userId()") Long userId) {

        Long authenticatedId = requireUserId(userId);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.ok(myInfoService.getMyInfo(authenticatedId)));
    }

    // 내 정보 수정
    @PutMapping
    public ResponseEntity<ApiResponse<MyInfoResponse>> updateMyInfo(
            @AuthenticationPrincipal(expression = "userId()") Long userId,
            @Valid @RequestBody MyInfoUpdateRequest request) {

        Long authenticatedId = requireUserId(userId);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.ok(myInfoService.updateMyInfo(authenticatedId, request)));
    }

    // 인증된 회원 ID 확인
    private Long requireUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new InsufficientAuthenticationException(
                    "유효한 로그인 정보가 없습니다."
            );
        }

        return userId;
    }
}