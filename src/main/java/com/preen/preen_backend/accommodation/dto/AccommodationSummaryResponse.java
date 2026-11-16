package com.preen.preen_backend.accommodation.dto;

import com.preen.preen_backend.accommodation.domain.Accommodation;

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
		int minNightlyPrice) {

	public static AccommodationSummaryResponse from(Accommodation accommodation) {
		return new AccommodationSummaryResponse(
				accommodation.getId(),
				accommodation.getName(),
				accommodation.getAddress(),
				accommodation.getRegion().getId(),
				accommodation.getRegion().getName(),
				accommodation.getCategory().getId(),
				accommodation.getCategory().getName(),
				accommodation.getThumbnailUrl(),
				accommodation.getMinNightlyPrice());
	}

}
