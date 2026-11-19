package com.preen.preen_backend.common;

import org.springframework.http.HttpStatus;

/**
 * feature별 enum(AuthErrorCode, ReservationErrorCode 등)이 구현한다.
 */
public interface TempErrorCode {

    HttpStatus getStatus();

    String getCode();

    String getMessage();
}
