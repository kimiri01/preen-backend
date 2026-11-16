package com.preen.preen_backend.accommodation.dto;

import com.preen.preen_backend.accommodation.domain.Category;

public record CategoryResponse(Long id, String code, String name) {

	public static CategoryResponse from(Category category) {
		return new CategoryResponse(category.getId(), category.getCode(), category.getName());
	}

}
