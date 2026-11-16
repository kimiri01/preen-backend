package com.preen.preen_backend.accommodation.domain;

/** 숙소 공개 상태. 공개(LISTED)인 숙소만 목록에 노출됩니다. */
public enum ListingStatus {
	DRAFT,   // 작성 중
	LISTED,  // 공개
	HIDDEN   // 숨김
}
