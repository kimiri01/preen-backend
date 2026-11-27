package com.preen.preen_backend.inventory.exception;

import com.preen.preen_backend.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

// 객실 재고 선점·조회·수정에서 발생하는 오류를 정의합니다. 응답 code는 enum 이름입니다.
public enum InventoryErrorCode implements ErrorCode {

    SOLD_OUT(HttpStatus.CONFLICT, "선택한 날짜에 예약 가능한 객실이 없습니다."),
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "날짜 범위가 올바르지 않습니다."),
    DATE_RANGE_TOO_LONG(HttpStatus.BAD_REQUEST, "날짜 범위가 너무 깁니다."),
    NOTHING_TO_UPDATE(HttpStatus.BAD_REQUEST, "수량, 가격, 판매 마감 중 하나 이상을 입력해 주세요."),
    TOTAL_COUNT_TOO_SMALL(HttpStatus.CONFLICT, "이미 예약되었거나 선점된 수량보다 적게 설정할 수 없습니다.");

    private final HttpStatus status;
    private final String message;

    InventoryErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus status() { return status; }
    public String message() { return message; }
}
