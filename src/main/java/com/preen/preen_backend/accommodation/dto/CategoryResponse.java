package com.preen.preen_backend.accommodation.dto;

import com.preen.preen_backend.accommodation.domain.AccommodationCategory;

/** code는 domain2 카테고리에 없어서 현재 null입니다. */
public record CategoryResponse(Long id, String code, String name) {

	public static CategoryResponse from(AccommodationCategory category) {
		return new CategoryResponse(category.getId(), null, category.getName());
	}

}
