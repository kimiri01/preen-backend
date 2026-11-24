package com.preen.preen_backend.common.storage;

import com.preen.preen_backend.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.http.HttpStatus;


@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum StorageErrorCode implements ErrorCode {


    EMPTY_FILE(HttpStatus.BAD_REQUEST, "파일이 비어 있습니다."),
    UNSUPPORTED_IMAGE_TYPE(HttpStatus.BAD_REQUEST, "JPEG, PNG, GIF, WebP 이미지만 올릴 수 있습니다."),
    IMAGE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "이미지 크기가 허용 범위를 넘었습니다."),
    INVALID_KEY(HttpStatus.BAD_REQUEST, "저장 경로가 올바르지 않습니다."),
    STORE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 저장에 실패했습니다.");




    private final HttpStatus status;
    private final String message;
}
