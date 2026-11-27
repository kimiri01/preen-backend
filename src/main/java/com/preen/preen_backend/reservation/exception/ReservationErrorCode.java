package com.preen.preen_backend.reservation.exception;

import com.preen.preen_backend.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

// 예약 생성·조회·취소에서 발생하는 오류를 정의합니다. 응답 code는 enum 이름입니다.
public enum ReservationErrorCode implements ErrorCode {

    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "예약을 찾을 수 없습니다."),
    ADULT_ONLY(HttpStatus.FORBIDDEN, "예약은 성인만 할 수 있습니다."),
    INVALID_DATES(HttpStatus.BAD_REQUEST, "체크인은 오늘 이후, 체크아웃은 체크인 다음 날 이후여야 합니다."),
    GUEST_COUNT_EXCEEDED(HttpStatus.BAD_REQUEST, "객실의 최대 인원을 넘었습니다."),
    ROOM_NOT_AVAILABLE(HttpStatus.CONFLICT, "예약할 수 없는 객실입니다."),
    NOT_CANCELLABLE(HttpStatus.CONFLICT, "취소할 수 없는 예약입니다."),
    SELF_RESERVATION_NOT_ALLOWED(HttpStatus.FORBIDDEN, "본인이 등록한 숙소는 예약할 수 없습니다.");

    private final HttpStatus status;
    private final String message;

    ReservationErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus status() { return status; }
    public String message() { return message; }
}
