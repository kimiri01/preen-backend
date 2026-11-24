package com.preen.preen_backend.auth.exception;

import com.preen.preen_backend.common.exception.BusinessException;

// 잘못된 인증번호 입력 횟수가 롤백되지 않도록 구분하는 예외입니다.
public class InvalidVerificationCodeException extends BusinessException {
    public InvalidVerificationCodeException(AuthErrorCode errorCode) { super(errorCode); }
}
