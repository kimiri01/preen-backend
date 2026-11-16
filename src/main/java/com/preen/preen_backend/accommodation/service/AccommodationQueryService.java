package com.preen.preen_backend.accommodation.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.preen.preen_backend.accommodation.domain.Accommodation;
import com.preen.preen_backend.accommodation.domain.ListingStatus;
import com.preen.preen_backend.accommodation.dto.AccommodationSummaryResponse;
import com.preen.preen_backend.accommodation.dto.CategoryResponse;
import com.preen.preen_backend.accommodation.dto.RegionResponse;
import com.preen.preen_backend.accommodation.repository.AccommodationRepository;
import com.preen.preen_backend.accommodation.repository.CategoryRepository;
import com.preen.preen_backend.accommodation.repository.RegionRepository;
import com.preen.preen_backend.common.response.PageResponse;

import lombok.RequiredArgsConstructor;

/** 카테고리·지역·숙소 "조회" 전용 서비스 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccommodationQueryService {

	private static final int MAX_PAGE_SIZE = 50;

	private final CategoryRepository categoryRepository;
	private final RegionRepository regionRepository;
	private final AccommodationRepository accommodationRepository;

	public List<CategoryResponse> getCategories() {
		return categoryRepository.findAllByOrderBySortOrderAsc().stream()
				.map(CategoryResponse::from)
				.toList();
	}

	public List<RegionResponse> getRegions() {
		return regionRepository.findAllByOrderBySortOrderAsc().stream()
				.map(RegionResponse::from)
				.toList();
	}

	/**
	 * 공개(LISTED) 숙소를 최근 등록순으로 조회합니다.
	 * categoryId, regionId는 없으면(null) 해당 조건을 적용하지 않습니다.
	 */
	public PageResponse<AccommodationSummaryResponse> getAccommodations(Long categoryId, Long regionId, int page,
			int size) {
		// 잘못된 값이 와도 에러 대신 허용 범위로 보정합니다. (page 0 이상, size 1~50)
		int safePage = Math.max(page, 0);
		int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
		Pageable pageable = PageRequest.of(safePage, safeSize,
				Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));

		Page<Accommodation> result = accommodationRepository.search(ListingStatus.LISTED, categoryId, regionId,
				pageable);

		return PageResponse.of(result, AccommodationSummaryResponse::from);
	}

}
