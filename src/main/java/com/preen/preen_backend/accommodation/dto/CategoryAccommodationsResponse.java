package com.preen.preen_backend.accommodation.dto;

import java.util.List;

import com.preen.preen_backend.accommodation.domain.AccommodationCategory;

/** 전체 숙소 화면의 카테고리 하나와 그 숙소 목록 */
public record CategoryAccommodationsResponse(
		Long categoryId,
		String categoryCode,
		String categoryName,
		List<AccommodationSummaryResponse> accommodations) {

	public static CategoryAccommodationsResponse of(AccommodationCategory category,
			List<AccommodationSummaryResponse> accommodations) {
		return new CategoryAccommodationsResponse(
				category.getId(),
				category.getCode(),
				category.getName(),
				accommodations);
	}

}
