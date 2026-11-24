package com.preen.preen_backend.accommodation.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.preen.preen_backend.accommodation.dto.AccommodationSummaryResponse;
import com.preen.preen_backend.accommodation.dto.CategoryAccommodationsResponse;
import com.preen.preen_backend.accommodation.service.AccommodationCardQueryService;
import com.preen.preen_backend.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

/** 메인 페이지(전체 숙소 묶음·최근 본 숙소) 전용 API */
@RestController
@RequestMapping("/api/accommodations")
@RequiredArgsConstructor
public class AccommodationCardController {

	private final AccommodationCardQueryService accommodationCardQueryService;

	/** 카테고리별 숙소 묶음 조회 (GET /api/accommodations/by-category) */
	@GetMapping("/by-category")
	public ApiResponse<List<CategoryAccommodationsResponse>> getAccommodationsByCategory(
			@RequestParam(name = "size", defaultValue = "6") int size) {
		return ApiResponse.ok(accommodationCardQueryService.getAccommodationsByCategory(size));
	}

	/** 최근 본 숙소 조회 (GET /api/accommodations/recent) */
	@GetMapping("/recent")
	public ApiResponse<List<AccommodationSummaryResponse>> getRecentlyViewed(
			@RequestParam(name = "ids", required = false) List<Long> ids) {
		return ApiResponse.ok(accommodationCardQueryService.getRecentlyViewed(ids));
	}

}
