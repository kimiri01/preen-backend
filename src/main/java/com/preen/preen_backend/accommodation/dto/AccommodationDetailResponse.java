package com.preen.preen_backend.accommodation.dto;

import java.util.List;

/**
 * [kny] 숙소 상세페이지 (GET /api/accommodations/{id})
 * 상단 기본 정보(사진·이름·유형/지역·주소)와 "숙소 소개", "시설/서비스"를 담습니다.
 * 목록 카드(AccommodationSummaryResponse)와 같은 규칙(숫자 ID, categoryId·regionId)을 따릅니다.
 */
public record AccommodationDetailResponse(
		Long id,
		String name,
		/** 숙소 소개. 첫 줄은 화면에서 강조 문구로 보여 줍니다. 없으면 null */
		String description,
		String address,
		Long regionId,
		String regionName,
		Long categoryId,
		String categoryName,
		/** sort_order 순서. 첫 번째가 대표 사진이고, 사진이 없으면 빈 배열 */
		List<String> imageUrls,
		/** 시설/서비스 이름 목록 (예: 와이파이, 주차). 없으면 빈 배열 */
		List<String> amenities) {
}
