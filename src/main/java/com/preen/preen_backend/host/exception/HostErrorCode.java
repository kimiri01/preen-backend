package com.preen.preen_backend.host.exception;

import com.preen.preen_backend.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;


@Getter
@RequiredArgsConstructor
public enum HostErrorCode implements ErrorCode {


    INVALID_REGISTRATION_NO(HttpStatus.BAD_REQUEST, "사업자등록번호 형식이 올바르지 않습니다."),
    BUSINESS_ALREADY_REGISTERED(HttpStatus.CONFLICT,  "이미 사업자를 등록한 회원입니다."),
    DUPLICATE_REGISTRATION_NO(HttpStatus.CONFLICT, "이미 등록된 사업자등록번호입니다."),
    BUSINESS_NOT_FOUND(HttpStatus.NOT_FOUND,  "등록된 사업자 정보가 없습니다.");


    private final HttpStatus status;
    private final String message;


    public HttpStatus status() { return status; }
    public String message() { return message; }


}
