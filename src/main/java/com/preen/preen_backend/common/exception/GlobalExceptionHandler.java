package com.preen.preen_backend.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import com.preen.preen_backend.common.response.ApiResponse;
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

    // yunjeong 26/11/18 추가 ----- 예약·재고 등 기능별 예외(BaseException)의 HTTP 상태, 코드와 메시지를 응답에 반영합니다.
    @ExceptionHandler(com.preen.preen_backend.common.BaseException.class)
    ResponseEntity<ApiResponse<Void>> base(com.preen.preen_backend.common.BaseException exception) {
        com.preen.preen_backend.common.ErrorCode code = exception.getErrorCode();
        return ResponseEntity.status(code.getStatus()).body(ApiResponse.failure(code.getCode(), code.getMessage()));
    }

    // 요청 본문 형식이나 입력 검증 오류를 400 응답으로 변환합니다.
    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    ResponseEntity<ApiResponse<Void>> invalidInput(Exception exception) {
        return ResponseEntity.badRequest().body(ApiResponse.failure(CommonErrorCode.INVALID_INPUT));
    }

    // 서비스에서 던진 ResponseStatusException(예: 숙소 검색 조건 오류)도 공통 응답 형식으로 바꿉니다.
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ApiResponse<Void>> responseStatus(ResponseStatusException exception) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        String code = status == HttpStatus.BAD_REQUEST ? CommonErrorCode.INVALID_INPUT.name() : status.name();
        String message = exception.getReason() != null ? exception.getReason() : status.getReasonPhrase();
        return ResponseEntity.status(status).body(ApiResponse.failure(code, message));
    }
}
