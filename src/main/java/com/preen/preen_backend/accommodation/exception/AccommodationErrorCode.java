package com.preen.preen_backend.accommodation.exception;

import com.preen.preen_backend.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.http.HttpStatus;


@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum AccommodationErrorCode implements ErrorCode {
    ACCOMMODATION_NOT_FOUND(HttpStatus.NOT_FOUND, "숙소를 찾을 수 없습니다."),
    NOT_OWNER(HttpStatus.FORBIDDEN, "본인 소유의 숙소가 아닙니다."),
    REGION_NOT_FOUND(HttpStatus.BAD_REQUEST, "존재하지 않는 지역입니다."),
    AMENITY_NOT_FOUND(HttpStatus.BAD_REQUEST, "존재하지 않는 편의시설이 포함되어 있습니다."),
    ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "객실을 찾을 수 없습니다."),
    INVALID_CAPACITY(HttpStatus.BAD_REQUEST, "최대 인원은 기준 인원 이상이어야 합니다."),
    NOT_READY_TO_LIST(HttpStatus.BAD_REQUEST, "판매 중인 객실과 환불정책을 등록해야 숙소를 공개할 수 있습니다."),
    DUPLICATE_REFUND_RULE(HttpStatus.BAD_REQUEST, "같은 일수의 환불 규칙이 중복되었습니다."),
    CATEGORY_NOT_FOUND(HttpStatus.BAD_REQUEST, "존재하지 않는 카테고리입니다."),
    LAST_ACTIVE_ROOM(HttpStatus.CONFLICT, "공개 중인 숙소의 마지막 판매 객실은 중지할 수 없습니다. 숙소를 먼저 숨겨 주세요.");


    private final HttpStatus status;
    private final String message;



}
