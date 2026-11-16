package com.feature.common.exception;

import org.springframework.http.ResponseEntity;
import com.feature.common.response.ApiResponse;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// 컨트롤러에서 발생한 업무·입력 오류를 공통 API 응답으로 변환합니다.
@RestControllerAdvice
public class GlobalExceptionHandler {
    // 업무 오류의 HTTP 상태, 코드와 메시지를 응답에 반영합니다.
    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiResponse<Void>> business(BusinessException exception) {
        ErrorCode code = exception.getErrorCode();
        return ResponseEntity.status(code.status()).body(ApiResponse.failure(code));
    }

    // 요청 본문 형식이나 입력 검증 오류를 400 응답으로 변환합니다.
    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    ResponseEntity<ApiResponse<Void>> invalidInput(Exception exception) {
        return ResponseEntity.badRequest().body(ApiResponse.failure(CommonErrorCode.INVALID_INPUT));
    }
}
