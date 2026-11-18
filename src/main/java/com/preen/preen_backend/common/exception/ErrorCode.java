package com.preen.preen_backend.common.exception;

import org.springframework.http.HttpStatus;

// 오류 코드가 제공할 HTTP 상태와 안내 메시지를 정의합니다.
public interface ErrorCode {
    HttpStatus status();
    String message();
    String name();
}
