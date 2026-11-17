package com.preen.preen_backend.accommodation.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.preen.preen_backend.accommodation.dto.AccommodationSummaryResponse;
import com.preen.preen_backend.accommodation.service.AccommodationQueryService;
import com.preen.preen_backend.common.response.ApiResponse;
import com.preen.preen_backend.common.response.PageResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/accommodations")
@RequiredArgsConstructor
public class AccommodationController {

	private final AccommodationQueryService accommodationQueryService;

	/**
	 * GET /api/accommodations?categoryId=&regionId=&page=&size=
	 * categoryId, regionId를 모두 생략하면 전체 숙소를 조회합니다.
	 */
	@GetMapping
	public ApiResponse<PageResponse<AccommodationSummaryResponse>> getAccommodations(
			@RequestParam(name = "categoryId", required = false) Long categoryId,
			@RequestParam(name = "regionId", required = false) Long regionId,
			@RequestParam(name = "page", defaultValue = "0") int page,
			@RequestParam(name = "size", defaultValue = "20") int size) {
		return ApiResponse.ok(accommodationQueryService.getAccommodations(categoryId, regionId, page, size));
	}

	/**
	 * GET /api/accommodations/recent?ids=5,3,9
	 * "최근 본 숙소" 조회. ids는 최근에 본 순서대로 쉼표로 구분해 보냅니다.
	 */
	@GetMapping("/recent")
	public ApiResponse<List<AccommodationSummaryResponse>> getRecentlyViewed(
			@RequestParam(name = "ids", required = false) List<Long> ids) {
		return ApiResponse.ok(accommodationQueryService.getRecentlyViewed(ids));
	}

}
