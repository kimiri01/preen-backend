package com.feature.common.response;

import com.feature.common.exception.ErrorCode;

// API 성공 결과와 오류를 공통 응답 형식으로 전달합니다.
public record ApiResponse<T>(boolean success, T data, String code, String message) {
    // 성공 데이터를 공통 응답에 담습니다.
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, null);
    }

    // 오류 코드와 메시지를 공통 응답에 담습니다.
    public static ApiResponse<Void> failure(ErrorCode errorCode) {
        return new ApiResponse<>(false, null, errorCode.name(), errorCode.message());
    }
}
