package com.preen.preen_backend.auth.exception;

import com.preen.preen_backend.common.exception.BusinessException;

// 로그인 실패 응답을 보내면서도 실패 기록은 DB에 남기기 위한 예외입니다.
public class LoginRejectedException extends BusinessException {
    public LoginRejectedException(AuthErrorCode errorCode) {
        super(errorCode);
    }
}
