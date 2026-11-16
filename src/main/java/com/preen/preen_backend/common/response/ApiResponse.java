package com.preen.preen_backend.common.response;

/**
 * 모든 API가 공통으로 사용하는 응답 형식입니다. (기능명세서 COMMON-004)
 * 예: { "success": true, "code": "OK", "message": "요청이 성공했습니다.", "data": ... }
 */
public record ApiResponse<T>(boolean success, String code, String message, T data) {

	public static <T> ApiResponse<T> ok(T data) {
		return new ApiResponse<>(true, "OK", "요청이 성공했습니다.", data);
	}

}
