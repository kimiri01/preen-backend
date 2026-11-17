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

/**
 * [kny] 메인 페이지 전용 조회 서비스: "전체 숙소" 카테고리 묶음, "최근 본 숙소".
 * 팀원 서비스(AccommodationQueryService)를 고치지 않고 따로 두었습니다.
 * 카드 한 장의 모양(카테고리명·지역명·사진·최저 요금)은 팀원 서비스의 toSummaries를 그대로 써서
 * 검색 목록과 메인 카드가 같은 규칙으로 만들어집니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccommodationCardQueryService {

	private static final int MAX_PAGE_SIZE = 50;
	private static final int MAX_RECENT_SIZE = 20;

	private final CategoryRepository categoryRepository;
	private final AccommodationCardRepository accommodationCardRepository;
	private final AccommodationQueryService accommodationQueryService;

	/**
	 * "전체 숙소" 묶음. 코드가 있는(메인 화면 버튼이 있는) 카테고리만 표시 순서대로 돌면서
	 * 각각 공개 숙소를 최근 등록순으로 size개(1~50)씩 붙입니다.
	 */
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

	/**
	 * "최근 본 숙소". 프론트가 브라우저에 저장해 둔 숙소 ID 목록을 받아 숙소 정보를 돌려줍니다.
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
