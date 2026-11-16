package com.preen.preen_backend.accommodation.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.preen.preen_backend.accommodation.dto.CategoryResponse;
import com.preen.preen_backend.accommodation.service.AccommodationQueryService;
import com.preen.preen_backend.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

	private final AccommodationQueryService accommodationQueryService;

	/** GET /api/categories : 숙박 유형 카테고리 목록 */
	@GetMapping
	public ApiResponse<List<CategoryResponse>> getCategories() {
		return ApiResponse.ok(accommodationQueryService.getCategories());
	}

}
