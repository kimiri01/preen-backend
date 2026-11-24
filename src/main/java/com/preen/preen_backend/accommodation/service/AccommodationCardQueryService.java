package com.preen.preen_backend.accommodation.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.preen.preen_backend.accommodation.domain.Accommodation;
import com.preen.preen_backend.accommodation.domain.Accommodation.ListingStatus;
import com.preen.preen_backend.accommodation.dto.AccommodationSummaryResponse;
import com.preen.preen_backend.accommodation.dto.CategoryAccommodationsResponse;
import com.preen.preen_backend.accommodation.repository.AccommodationCardRepository;
import com.preen.preen_backend.accommodation.repository.CategoryRepository;

import lombok.RequiredArgsConstructor;

/** 메인 페이지(전체 숙소 묶음·최근 본 숙소) 조회 서비스 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccommodationCardQueryService {

	private static final int MAX_PAGE_SIZE = 50;
	private static final int MAX_RECENT_SIZE = 20;

	private final CategoryRepository categoryRepository;
	private final AccommodationCardRepository accommodationCardRepository;
	private final AccommodationQueryService accommodationQueryService;

	/** 카테고리별로 공개 숙소를 size개씩 묶어 반환합니다. */
	public List<CategoryAccommodationsResponse> getAccommodationsByCategory(int size) {
		int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
		Pageable firstPage = PageRequest.of(0, safeSize,
				Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));

		return categoryRepository.findAllByOrderBySortOrderAsc().stream()
				.filter(category -> category.getCode() != null)
				.map(category -> CategoryAccommodationsResponse.of(category,
						accommodationQueryService.toSummaries(accommodationCardRepository
								.findByListingStatusAndCategoryId(ListingStatus.LISTED, category.getId(), firstPage))))
				.toList();
	}

	/** 받은 숙소 ID 순서대로 최근 본 숙소 정보를 반환합니다. */
	public List<AccommodationSummaryResponse> getRecentlyViewed(List<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return List.of();
		}
		List<Long> requestedIds = ids.stream()
				.filter(Objects::nonNull)
				.distinct()
				.limit(MAX_RECENT_SIZE)
				.toList();

		Map<Long, Accommodation> foundById = accommodationCardRepository
				.findByIdInAndListingStatus(requestedIds, ListingStatus.LISTED).stream()
				.collect(Collectors.toMap(Accommodation::getId, Function.identity()));
		List<Accommodation> ordered = requestedIds.stream()
				.map(foundById::get)
				.filter(Objects::nonNull)
				.toList();
		return accommodationQueryService.toSummaries(ordered);
	}

}
