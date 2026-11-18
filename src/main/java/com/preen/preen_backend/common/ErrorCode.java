package com.preen.preen_backend.common;

import org.springframework.http.HttpStatus;

/**
 * feature별 enum(AuthErrorCode, ReservationErrorCode 등)이 구현한다.
 */
public interface ErrorCode {

    HttpStatus getStatus();

    String getCode();

    String getMessage();
}
