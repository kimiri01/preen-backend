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

/**
 * [kny] 메인 페이지 전용 API. 팀원 컨트롤러(AccommodationController)를 고치지 않으려고 따로 두었습니다.
 * 주소는 같은 /api/accommodations 아래이지만 경로(/by-category, /recent)가 달라 서로 부딪히지 않습니다.
 * 나중에 상세 조회(/api/accommodations/{id})가 생겨도 글자로 된 경로가 먼저 매칭됩니다.
 */
@RestController
@RequestMapping("/api/accommodations")
@RequiredArgsConstructor
public class AccommodationCardController {

	private final AccommodationCardQueryService accommodationCardQueryService;

	/**
	 * GET /api/accommodations/by-category?size=6
	 * "전체 숙소" 화면용. 카테고리마다 숙소를 size개(기본 6, 최대 50)씩 묶어서 한 번에 돌려줍니다.
	 */
	@GetMapping("/by-category")
	public ApiResponse<List<CategoryAccommodationsResponse>> getAccommodationsByCategory(
			@RequestParam(name = "size", defaultValue = "6") int size) {
		return ApiResponse.ok(accommodationCardQueryService.getAccommodationsByCategory(size));
	}

	/**
	 * GET /api/accommodations/recent?ids=5,3,9
	 * "최근 본 숙소" 조회. ids는 최근에 본 순서대로 쉼표로 구분해 보냅니다. 없으면 빈 배열.
	 */
	@GetMapping("/recent")
	public ApiResponse<List<AccommodationSummaryResponse>> getRecentlyViewed(
			@RequestParam(name = "ids", required = false) List<Long> ids) {
		return ApiResponse.ok(accommodationCardQueryService.getRecentlyViewed(ids));
	}

}
