package com.preen.preen_backend.accommodation.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.preen.preen_backend.accommodation.domain.Accommodation;
import com.preen.preen_backend.accommodation.domain.ListingStatus;
import com.preen.preen_backend.accommodation.dto.AccommodationSummaryResponse;
import com.preen.preen_backend.accommodation.dto.CategoryAccommodationsResponse;
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
	private static final int MAX_RECENT_SIZE = 20;

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

	/**
	 * "전체 숙소" 화면용. 카테고리마다 공개 숙소를 최근 등록순으로 size개씩 묶어서 돌려줍니다.
	 * - 카테고리는 화면 표시 순서(sort_order)대로 정렬합니다.
	 * - 숙소가 없는 카테고리도 빈 목록으로 포함합니다. (화면에서 숨길지 프론트가 결정)
	 * - 프론트는 한 번만 요청하고, 서버 안에서 카테고리마다 DB를 조회합니다.
	 */
	public List<CategoryAccommodationsResponse> getAccommodationsByCategory(int size) {
		int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
		Pageable firstPage = PageRequest.of(0, safeSize,
				Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));

		return categoryRepository.findAllByOrderBySortOrderAsc().stream()
				.map(category -> CategoryAccommodationsResponse.of(category,
						accommodationRepository
								.findByListingStatusAndCategory_Id(ListingStatus.LISTED, category.getId(), firstPage)
								.stream()
								.map(AccommodationSummaryResponse::from)
								.toList()))
				.toList();
	}

	/**
	 * "최근 본 숙소" 조회. 프론트가 브라우저에 저장해 둔 숙소 ID 목록을 받아 숙소 정보를 돌려줍니다.
	 * - 받은 ID 순서(최근에 본 순서) 그대로 반환합니다.
	 * - 중복 ID는 한 번만, 최대 20개까지만 처리합니다.
	 * - 삭제됐거나 공개(LISTED)가 아닌 숙소는 결과에서 빠집니다.
	 */
	public List<AccommodationSummaryResponse> getRecentlyViewed(List<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return List.of();
		}
		List<Long> requestedIds = ids.stream()
				.filter(Objects::nonNull)
				.distinct()
				.limit(MAX_RECENT_SIZE)
				.toList();

		// DB는 순서를 보장하지 않으므로, ID로 찾을 수 있게 Map에 담은 뒤 요청 순서대로 다시 꺼냅니다.
		Map<Long, Accommodation> foundById = accommodationRepository
				.findByIdInAndListingStatus(requestedIds, ListingStatus.LISTED).stream()
				.collect(Collectors.toMap(Accommodation::getId, Function.identity()));

		return requestedIds.stream()
				.map(foundById::get)
				.filter(Objects::nonNull)
				.map(AccommodationSummaryResponse::from)
				.toList();
	}

}
