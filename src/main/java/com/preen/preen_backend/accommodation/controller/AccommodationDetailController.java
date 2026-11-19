package com.preen.preen_backend.accommodation.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.preen.preen_backend.accommodation.dto.AccommodationDetailResponse;
import com.preen.preen_backend.accommodation.service.AccommodationDetailQueryService;
import com.preen.preen_backend.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

/**
 * [kny] 숙소 상세페이지 API.
 * {accommodationId}는 숫자만 받도록 제한해서 /by-category, /recent 같은 글자 경로와 겹치지 않습니다.
 */
@RestController
@RequestMapping("/api/accommodations")
@RequiredArgsConstructor
public class AccommodationDetailController {

	private final AccommodationDetailQueryService accommodationDetailQueryService;

	/** GET /api/accommodations/{accommodationId} : 숙소 기본 정보 */
	@GetMapping("/{accommodationId:\\d+}")
	public ApiResponse<AccommodationDetailResponse> getDetail(
			@PathVariable("accommodationId") Long accommodationId) {
		return ApiResponse.ok(accommodationDetailQueryService.getDetail(accommodationId));
	}
}
