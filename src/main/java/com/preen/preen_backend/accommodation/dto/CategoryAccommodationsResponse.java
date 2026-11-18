package com.preen.preen_backend.accommodation.dto;

import java.util.List;

import com.preen.preen_backend.accommodation.domain.AccommodationCategory;

/**
 * [kny] "전체 숙소" 화면의 카테고리 묶음 하나.
 * 카테고리 정보와 그 카테고리의 숙소 목록(최대 size개)을 함께 담습니다.
 */
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
