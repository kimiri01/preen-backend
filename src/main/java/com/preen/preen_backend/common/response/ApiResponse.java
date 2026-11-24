package com.preen.preen_backend.common.response;

import com.preen.preen_backend.common.exception.ErrorCode;

/** 모든 API가 공통으로 사용하는 응답 형식 */
public record ApiResponse<T>(boolean success, String code, String message, T data) {

	/** 성공 데이터를 공통 응답에 담습니다. */
	public static <T> ApiResponse<T> ok(T data) {
		return new ApiResponse<>(true, "OK", "요청이 성공했습니다.", data);
	}

	/** 오류 코드와 메시지를 공통 응답에 담습니다. (GlobalExceptionHandler에서 사용) */
	public static ApiResponse<Void> failure(ErrorCode errorCode) {
		return new ApiResponse<>(false, errorCode.name(), errorCode.message(), null);
	}

	/** ErrorCode 없이 코드와 메시지를 직접 담습니다. (ResponseStatusException 변환용) */
	public static ApiResponse<Void> failure(String code, String message) {
		return new ApiResponse<>(false, code, message, null);
	}

}
