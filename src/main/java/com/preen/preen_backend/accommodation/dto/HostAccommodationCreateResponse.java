package com.preen.preen_backend.accommodation.dto;

/**
 * [kny] 호스트 숙소 등록 결과.
 * 새 숙소는 작성 중(DRAFT) 상태로 만들어지며, 공개(LISTED) 전환은 HOST-003에서 합니다.
 */
public record HostAccommodationCreateResponse(
		Long id,
		String name,
		String listingStatus) {
}
