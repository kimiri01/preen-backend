package com.preen.preen_backend.accommodation.dto;

/** 숙소 목록의 카드 한 장에 필요한 정보 */
public record AccommodationSummaryResponse(
		Long id,
		String name,
		String address,
		Long regionId,
		String regionName,
		Long categoryId,
		String categoryName,
		String thumbnailUrl,
		Integer minNightlyPrice) {
}
