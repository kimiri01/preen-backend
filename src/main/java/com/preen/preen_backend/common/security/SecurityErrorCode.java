package com.preen.preen_backend.common.security;

import com.preen.preen_backend.common.exception.ErrorCode;
import com.preen.preen_backend.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import tools.jackson.databind.ObjectMapper;

@RequiredArgsConstructor
enum SecurityErrorCode implements ErrorCode {
    ACCESS_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Access Token이 만료됐어요."),
    SESSION_INVALID(HttpStatus.UNAUTHORIZED, "다시 로그인해 주세요."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "이 기능에 접근할 권한이 없어요."),
    SESSION_LOOKUP_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "로그인 상태를 확인할 수 없어요. 잠시 후 다시 시도해 주세요.");

    private final HttpStatus status;
    private final String message;

    public HttpStatus status() {
        return status;
    }

    public String message() {
        return message;
    }

    void write(HttpServletResponse response, ObjectMapper mapper) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), ApiResponse.failure(this));
    }
}
