package com.preen.preen_backend.accommodation.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.preen.preen_backend.accommodation.dto.RegionResponse;
import com.preen.preen_backend.accommodation.service.AccommodationQueryService;
import com.preen.preen_backend.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/regions")
@RequiredArgsConstructor
public class RegionController {

	private final AccommodationQueryService accommodationQueryService;

	/** GET /api/regions : 지역 목록 ("지역으로 찾기"에서 사용) */
	@GetMapping
	public ApiResponse<List<RegionResponse>> getRegions() {
		return ApiResponse.ok(accommodationQueryService.getRegions());
	}

}
