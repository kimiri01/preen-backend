package com.preen.preen_backend.common.response;

/** 입력 검증 실패 시 data에 담는 필드별 오류 (예: { "field": "guestPhone", "message": "..." }) */
public record FieldErrorResponse(String field, String message) {
}
