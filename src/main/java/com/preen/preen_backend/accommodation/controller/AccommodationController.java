package com.preen.preen_backend.accommodation.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
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
	 * GET /api/accommodations?keyword=&categoryId=&regionId=&guestCount=&checkIn=&checkOut=&page=&size=
	 * keyword, categoryId, regionId, guestCount, checkIn, checkOut은 모두 선택이고, 함께 쓰면 모두 만족하는 숙소만 나옵니다.
	 * keyword는 숙소명 검색어(100자 이하), guestCount는 1 이상입니다.
	 * checkIn과 checkOut은 함께 보내야 하며 형식은 yyyy-MM-dd입니다.
	 * 날짜를 주면 그 기간에 예약 가능한 숙소만 나옵니다.
	 */
	@GetMapping
	public ApiResponse<PageResponse<AccommodationSummaryResponse>> getAccommodations(
			@RequestParam(name = "keyword", required = false) String keyword,
			@RequestParam(name = "categoryId", required = false) Long categoryId,
			@RequestParam(name = "regionId", required = false) Long regionId,
			@RequestParam(name = "guestCount", required = false) Integer guestCount,
			@RequestParam(name = "checkIn", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
			@RequestParam(name = "checkOut", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
			@RequestParam(name = "page", defaultValue = "0") int page,
			@RequestParam(name = "size", defaultValue = "20") int size) {
		return ApiResponse.ok(
				accommodationQueryService.getAccommodations(keyword, categoryId, regionId, guestCount, checkIn,
						checkOut, page, size));
	}

}
