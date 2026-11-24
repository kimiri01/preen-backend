package com.preen.preen_backend.accommodation.dto;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * [kny] 호스트 숙소 등록 요청 (POST /api/host/accommodations, 기능명세서 HOST-002)
 * 필수: 지역, 카테고리, 이름(100자 이하), 주소(255자 이하), 체크인·체크아웃 시각
 * 선택: 숙소 소개(5,000자 이하), 위도·경도, 이미지 URL(최대 20장), 편의시설 ID 목록
 * 시각은 "15:00" 또는 "15:00:00" 형식으로 보냅니다.
 */
public record HostAccommodationCreateRequest(
		@NotNull @Positive Long regionId,
		@NotNull @Positive Long categoryId,
		@NotBlank @Size(max = 100) String name,
		/** 숙소 소개. 상세페이지에서 첫 줄은 강조 문구로, 나머지 줄은 소개 문단으로 보여 줍니다. */
		@Size(max = 5000) String description,
		@NotBlank @Size(max = 255) String address,
		@DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
		@DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
		@NotNull LocalTime checkInTime,
		@NotNull LocalTime checkOutTime,
		/** 이미지 업로드(COMMON-006)로 먼저 올리고 받은 URL. 첫 번째가 대표 사진 */
		@Size(max = 20) List<@NotBlank @Size(max = 500) String> imageUrls,
		/** 편의시설 ID 목록 (amenities 테이블) */
		@Size(max = 100) List<@NotNull @Positive Long> amenityIds) {
}
