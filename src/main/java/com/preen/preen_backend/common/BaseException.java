package com.preen.preen_backend.common;

public class BaseException extends RuntimeException {

    private final TempErrorCode tempErrorCode;

    public BaseException(TempErrorCode tempErrorCode) {
        super(tempErrorCode.getMessage());
        this.tempErrorCode = tempErrorCode;
    }

    public BaseException(TempErrorCode tempErrorCode, String message) {
        super(message);
        this.tempErrorCode = tempErrorCode;
    }

    public BaseException(TempErrorCode tempErrorCode, Throwable cause) {
        super(tempErrorCode.getMessage(), cause);
        this.tempErrorCode = tempErrorCode;
    }

    public TempErrorCode getErrorCode() {
        return tempErrorCode;
    }
}
