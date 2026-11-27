package com.preen.preen_backend.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import com.preen.preen_backend.common.response.ApiResponse;
import com.preen.preen_backend.common.response.FieldErrorResponse;
import java.util.List;
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

    // @Valid 검증 실패: 필드별 오류를 data에, 첫 번째 필드 오류 문구를 message에 담아 400으로 응답합니다.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<List<FieldErrorResponse>>> invalidArgument(MethodArgumentNotValidException exception) {
        List<FieldErrorResponse> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldErrorResponse(error.getField(),
                        error.getDefaultMessage() != null ? error.getDefaultMessage()
                                : CommonErrorCode.INVALID_INPUT.message()))
                .toList();
        String message = errors.isEmpty() ? CommonErrorCode.INVALID_INPUT.message() : errors.get(0).message();
        return ResponseEntity.badRequest().body(ApiResponse.failure(CommonErrorCode.INVALID_INPUT, message, errors));
    }

    // 요청 본문 형식이나 파라미터 오류를 400 응답으로 변환합니다.
    @ExceptionHandler({HttpMessageNotReadableException.class,
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
