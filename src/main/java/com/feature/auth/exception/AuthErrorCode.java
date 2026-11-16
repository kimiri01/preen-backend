package com.feature.auth.exception;

import com.feature.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

// 이메일 인증과 회원가입에서 발생하는 오류를 정의합니다.
public enum AuthErrorCode implements ErrorCode {
    SIGNUP_PROOF_INVALID(HttpStatus.BAD_REQUEST, "이메일 인증이 유효하지 않습니다. 다시 인증해 주세요."),
    PASSWORD_INVALID(HttpStatus.BAD_REQUEST, "비밀번호 조건 또는 비밀번호 확인이 올바르지 않습니다."),
    AGE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "만 14세 이상부터 가입할 수 있습니다."),
    TERMS_INVALID(HttpStatus.BAD_REQUEST, "약관 버전과 필수 동의 항목을 확인해 주세요."),
    EMAIL_ALREADY_REGISTERED(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
    RESEND_TOO_SOON(HttpStatus.TOO_MANY_REQUESTS, "인증번호는 60초 후 다시 받을 수 있습니다."),
    CODE_EXPIRED(HttpStatus.BAD_REQUEST, "인증번호가 만료되었습니다. 다시 발송해 주세요."),
    CODE_INVALID(HttpStatus.BAD_REQUEST, "인증번호가 올바르지 않습니다."),
    CODE_ATTEMPTS_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "인증 시도 횟수를 초과했습니다. 다시 발송해 주세요."),
    CODE_ALREADY_VERIFIED(HttpStatus.CONFLICT, "이미 인증이 완료된 요청입니다."),
    MAIL_SEND_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "인증메일을 발송하지 못했습니다. 잠시 후 다시 시도해 주세요.");

    private final HttpStatus status;
    private final String message;
    AuthErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
    public HttpStatus status() { return status; }
    public String message() { return message; }
}
