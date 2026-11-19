
package com.preen.preen_backend.myPage;

import com.preen.preen_backend.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum MyInfoErrorCode implements ErrorCode {

    USER_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "회원을 찾을 수 없습니다."
    ),

    USER_NOT_ACTIVE(
            HttpStatus.FORBIDDEN,
            "활성 상태의 회원이 아닙니다."
    ),

    PERSONAL_INFO_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "회원 개인정보를 찾을 수 없습니다."
    );

    private final HttpStatus status;
    private final String message;

    MyInfoErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    @Override
    public HttpStatus status() {
        return status;
    }

    @Override
    public String message() {
        return message;
    }
}
