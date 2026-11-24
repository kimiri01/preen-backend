package com.preen.preen_backend.accommodation.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.preen.preen_backend.accommodation.dto.HostAccommodationCreateRequest;
import com.preen.preen_backend.accommodation.dto.HostAccommodationCreateResponse;
import com.preen.preen_backend.accommodation.security.HostUserResolver;
import com.preen.preen_backend.accommodation.service.HostAccommodationCommandService;
import com.preen.preen_backend.common.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [kny] 호스트 숙소 관리 API (기능명세서 HOST-002). 이번에는 "등록"만 구현했습니다.
 * /api/host/** 는 공개 경로가 아니므로 로그인한 회원만 호출할 수 있습니다.
 */
@RestController
@RequestMapping("/api/host/accommodations")
@RequiredArgsConstructor
public class HostAccommodationController {

	private final HostUserResolver hostUserResolver;
	private final HostAccommodationCommandService hostAccommodationCommandService;

	/** POST /api/host/accommodations : 숙소 등록 (작성 중 DRAFT 상태로 생성) */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<HostAccommodationCreateResponse> create(
			Authentication authentication,
			@Valid @RequestBody HostAccommodationCreateRequest request) {
		Long hostUserId = hostUserResolver.requireUserId(authentication);
		return ApiResponse.ok(hostAccommodationCommandService.create(hostUserId, request));
	}
}
