package com.preen.preen_backend.accommodation.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.preen.preen_backend.accommodation.domain.Accommodation;
import com.preen.preen_backend.accommodation.domain.Accommodation.ListingStatus;
import com.preen.preen_backend.accommodation.domain.AccommodationCategory;
import com.preen.preen_backend.accommodation.domain.AccommodationImage;
import com.preen.preen_backend.accommodation.domain.Region;
import com.preen.preen_backend.accommodation.dto.AccommodationSummaryResponse;
import com.preen.preen_backend.accommodation.dto.CategoryResponse;
import com.preen.preen_backend.accommodation.dto.RegionResponse;
import com.preen.preen_backend.accommodation.repository.AccommodationImageRepository;
import com.preen.preen_backend.accommodation.repository.AccommodationRepository;
import com.preen.preen_backend.accommodation.repository.CategoryRepository;
import com.preen.preen_backend.accommodation.repository.RegionRepository;
import com.preen.preen_backend.accommodation.repository.RoomPriceRepository;
import com.preen.preen_backend.common.response.PageResponse;

import lombok.RequiredArgsConstructor;

/** 카테고리·지역·숙소 "조회" 전용 서비스 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccommodationQueryService {

	private static final int MAX_PAGE_SIZE = 50;
	private static final int MAX_KEYWORD_LENGTH = 100;
	/** 시·도 단계 지역 (domain2 Region.level) */
	private static final int LEVEL_PROVINCE = 1;

	private final CategoryRepository categoryRepository;
	private final RegionRepository regionRepository;
	private final AccommodationRepository accommodationRepository;
	private final AccommodationImageRepository accommodationImageRepository;
	private final RoomPriceRepository roomPriceRepository;

	public List<CategoryResponse> getCategories() {
		return categoryRepository.findAllByOrderBySortOrderAsc().stream()
				.map(CategoryResponse::from)
				.toList();
	}

	public List<RegionResponse> getRegions() {
		return regionRepository.findAllByLevelOrderByIdAsc(LEVEL_PROVINCE).stream()
				.map(RegionResponse::from)
				.toList();
	}

	/** 조건에 맞는 공개 숙소를 최근 등록순으로 조회합니다. */
	public PageResponse<AccommodationSummaryResponse> getAccommodations(String keyword, Long categoryId,
			Long regionId, Integer guestCount, LocalDate checkIn, LocalDate checkOut, int page, int size) {
		if (keyword != null && keyword.length() > MAX_KEYWORD_LENGTH) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"검색어는 " + MAX_KEYWORD_LENGTH + "자 이하여야 합니다.");
		}
		if (guestCount != null && guestCount < 1) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "인원은 1명 이상이어야 합니다.");
		}
		int safePage = Math.max(page, 0);
		int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
		Pageable pageable = PageRequest.of(safePage, safeSize,
				Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));

		boolean dateFilter = checkIn != null || checkOut != null;
		if (dateFilter) {
			if (checkIn == null || checkOut == null) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "checkIn과 checkOut은 함께 보내야 합니다.");
			}
			if (!checkOut.isAfter(checkIn)) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "checkOut은 checkIn보다 뒤여야 합니다.");
			}
		}
		long nights = dateFilter ? ChronoUnit.DAYS.between(checkIn, checkOut) : 0;
		LocalDate from = dateFilter ? checkIn : LocalDate.EPOCH;
		LocalDate to = dateFilter ? checkOut : LocalDate.EPOCH;

		Page<Accommodation> result = accommodationRepository.search(ListingStatus.LISTED, toNamePattern(keyword),
				categoryId, regionId, guestCount, dateFilter, from, to, nights, pageable);
		return new PageResponse<>(
				toSummaries(result.getContent()),
				result.getNumber(),
				result.getSize(),
				result.getTotalElements(),
				result.getTotalPages(),
				result.hasNext());
	}

	/** 검색어를 숙소명 LIKE 패턴으로 바꿉니다. */
	private static String toNamePattern(String keyword) {
		if (keyword == null || keyword.isBlank()) {
			return null;
		}
		String escaped = keyword.trim().replace("!", "!!").replace("%", "!%").replace("_", "!_");
		return "%" + escaped + "%";
	}

	/** 숙소 목록에 카테고리명·지역명·대표 사진·최저 요금을 붙입니다. */
	List<AccommodationSummaryResponse> toSummaries(List<Accommodation> accommodations) {
		if (accommodations.isEmpty()) {
			return List.of();
		}
		Map<Long, String> categoryNames = categoryRepository.findAll().stream()
				.collect(Collectors.toMap(AccommodationCategory::getId, AccommodationCategory::getName));
		Map<Long, String> regionNames = regionRepository.findAll().stream()
				.collect(Collectors.toMap(Region::getId, Region::getName));
		List<Long> ids = accommodations.stream().map(Accommodation::getId).toList();
		Map<Long, String> thumbnails = accommodationImageRepository
				.findAllByAccommodationIdInOrderBySortOrderAsc(ids).stream()
				.collect(Collectors.toMap(AccommodationImage::getAccommodationId, AccommodationImage::getImageUrl,
						(first, later) -> first));
		Map<Long, Integer> minPrices = roomPriceRepository.findMinNightlyPrices(ids).stream()
				.collect(Collectors.toMap(RoomPriceRepository.MinPrice::getAccommodationId,
						price -> price.getMinNightlyPrice().intValue()));

		Function<Accommodation, AccommodationSummaryResponse> mapper = a -> new AccommodationSummaryResponse(
				a.getId(),
				a.getName(),
				a.getAddress(),
				a.getRegionId(),
				regionNames.get(a.getRegionId()),
				a.getCategoryId(),
				categoryNames.get(a.getCategoryId()),
				thumbnails.get(a.getId()),
				minPrices.get(a.getId()));
		return accommodations.stream().map(mapper).toList();
	}

}
