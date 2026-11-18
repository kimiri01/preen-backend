package com.preen.preen_backend.accommodation.dto;

import com.preen.preen_backend.accommodation.domain.AccommodationCategory;

/** code는 메인 화면 버튼과 약속한 고정 코드입니다. 코드가 없는 카테고리는 null입니다. (V101) */
public record CategoryResponse(Long id, String code, String name) {

	public static CategoryResponse from(AccommodationCategory category) {
		return new CategoryResponse(category.getId(), category.getCode(), category.getName());
	}

}
