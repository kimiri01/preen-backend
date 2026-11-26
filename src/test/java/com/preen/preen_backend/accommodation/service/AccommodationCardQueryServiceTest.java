package com.preen.preen_backend.accommodation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.LongStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import com.preen.preen_backend.accommodation.domain.Accommodation;
import com.preen.preen_backend.accommodation.domain.Accommodation.ListingStatus;
import com.preen.preen_backend.accommodation.domain.AccommodationCategory;
import com.preen.preen_backend.accommodation.dto.AccommodationSummaryResponse;
import com.preen.preen_backend.accommodation.dto.CategoryAccommodationsResponse;
import com.preen.preen_backend.accommodation.repository.AccommodationCardRepository;
import com.preen.preen_backend.accommodation.repository.CategoryRepository;

/**
 * 메인 페이지 조회(AccommodationCardQueryService) 단위 테스트.
 * "전체 숙소" 카테고리 묶음과 "최근 본 숙소" 조회 규칙을 확인합니다.
 * 카드 정보 조합(toSummaries)은 AccommodationQueryServiceTest에서 따로 검사하므로 여기서는 가짜로 바꿉니다.
 */
class AccommodationCardQueryServiceTest {

	private final CategoryRepository categoryRepository = mock(CategoryRepository.class);
	private final AccommodationCardRepository accommodationCardRepository = mock(AccommodationCardRepository.class);
	private final AccommodationQueryService accommodationQueryService = mock(AccommodationQueryService.class);

	private AccommodationCardQueryService service;

	@BeforeEach
	void setUp() {
		service = new AccommodationCardQueryService(categoryRepository, accommodationCardRepository,
				accommodationQueryService);
		// 카드 변환은 "받은 숙소마다 id만 담은 카드"를 돌려주게 해서, 어떤 숙소가 어떤 순서로 넘어왔는지 확인합니다.
		when(accommodationQueryService.toSummaries(anyList())).thenAnswer(invocation -> {
			List<Accommodation> accommodations = invocation.getArgument(0);
			return accommodations.stream().map(AccommodationCardQueryServiceTest::card).toList();
		});
	}

	// ===== 전체 숙소: 카테고리별 묶음 =====

	@Test
	@DisplayName("코드가 있는 카테고리마다 공개 숙소를 묶어서, 카테고리 정렬 순서대로 돌려준다")
	void getAccommodationsByCategoryGroupsListedAccommodations() {
		// given: 카테고리 3개 중 1개는 코드가 없다 (메인 화면 버튼과 연결되지 않은 카테고리)
		AccommodationCategory hotel = category(1L, "HOTEL", "호텔·리조트");
		AccommodationCategory noCode = category(2L, null, "기타");
		AccommodationCategory hanok = category(3L, "HANOK", "한옥");
		when(categoryRepository.findAllByOrderBySortOrderAsc()).thenReturn(List.of(hotel, noCode, hanok));
		when(accommodationCardRepository.findByListingStatusAndCategoryId(eq(ListingStatus.LISTED), eq(1L), any()))
				.thenReturn(List.of(accommodation(10L), accommodation(11L)));
		when(accommodationCardRepository.findByListingStatusAndCategoryId(eq(ListingStatus.LISTED), eq(3L), any()))
				.thenReturn(List.of(accommodation(20L)));

		// when
		List<CategoryAccommodationsResponse> result = service.getAccommodationsByCategory(6);

		// then: 코드 없는 카테고리는 빠지고, 나머지는 순서대로 숙소가 묶인다
		assertEquals(2, result.size());
		assertEquals("HOTEL", result.get(0).categoryCode());
		assertEquals("호텔·리조트", result.get(0).categoryName());
		assertEquals(List.of(10L, 11L), ids(result.get(0).accommodations()));
		assertEquals("HANOK", result.get(1).categoryCode());
		assertEquals(List.of(20L), ids(result.get(1).accommodations()));
	}

	@Test
	@DisplayName("카테고리마다 첫 페이지를 요청한 개수만큼, 최근 등록순으로 조회한다")
	void getAccommodationsByCategoryRequestsFirstPageSortedByNewest() {
		when(categoryRepository.findAllByOrderBySortOrderAsc()).thenReturn(List.of(category(1L, "HOTEL", "호텔")));

		service.getAccommodationsByCategory(6);

		Pageable pageable = captureCategoryPageable();
		assertEquals(0, pageable.getPageNumber());
		assertEquals(6, pageable.getPageSize());
		assertEquals(Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")), pageable.getSort());
	}

	@Test
	@DisplayName("요청 개수는 1~50 사이로 보정한다 (0 → 1, 999 → 50)")
	void getAccommodationsByCategoryClampsSize() {
		when(categoryRepository.findAllByOrderBySortOrderAsc()).thenReturn(List.of(category(1L, "HOTEL", "호텔")));

		service.getAccommodationsByCategory(0);
		assertEquals(1, captureCategoryPageable().getPageSize());

		service.getAccommodationsByCategory(999);
		ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
		verify(accommodationCardRepository, times(2))
				.findByListingStatusAndCategoryId(any(), any(), captor.capture());
		assertEquals(50, captor.getAllValues().get(1).getPageSize());
	}

	// ===== 최근 본 숙소 =====

	@Test
	@DisplayName("ID 목록이 null이거나 비어 있으면 DB 조회 없이 빈 목록을 돌려준다")
	void getRecentlyViewedReturnsEmptyForNoIds() {
		assertTrue(service.getRecentlyViewed(null).isEmpty());
		assertTrue(service.getRecentlyViewed(List.of()).isEmpty());

		verifyNoInteractions(accommodationCardRepository);
	}

	@Test
	@DisplayName("요청한 ID 순서대로 돌려주고, 중복·null은 빼고, 공개되지 않아 못 찾은 숙소는 건너뛴다")
	void getRecentlyViewedKeepsRequestedOrder() {
		// given: 최근 본 순서는 3 → 1 → 2. 그중 2번은 숨김 처리돼서 DB에서 안 나온다.
		//        DB는 순서를 보장하지 않으므로 일부러 1, 3 순서로 돌려준다.
		List<Long> ids = new ArrayList<>(Arrays.asList(3L, null, 1L, 3L, 2L));
		when(accommodationCardRepository.findByIdInAndListingStatus(any(), eq(ListingStatus.LISTED)))
				.thenReturn(List.of(accommodation(1L), accommodation(3L)));

		// when
		List<AccommodationSummaryResponse> result = service.getRecentlyViewed(ids);

		// then: DB에는 중복·null을 뺀 [3, 1, 2]로 물어보고, 결과는 최근 본 순서 [3, 1]이다
		assertEquals(List.of(3L, 1L, 2L), captureRequestedIds());
		assertEquals(List.of(3L, 1L), ids(result));
	}

	@Test
	@DisplayName("최근 본 숙소는 최대 20개까지만 조회한다")
	void getRecentlyViewedLimitsToTwenty() {
		List<Long> ids = LongStream.rangeClosed(1, 25).boxed().toList();
		when(accommodationCardRepository.findByIdInAndListingStatus(any(), any())).thenReturn(List.of());

		service.getRecentlyViewed(ids);

		Collection<Long> requested = captureRequestedIds();
		assertEquals(20, requested.size());
		assertEquals(LongStream.rangeClosed(1, 20).boxed().toList(), requested);
	}

	// ===== 테스트 도우미 =====

	private Pageable captureCategoryPageable() {
		ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
		verify(accommodationCardRepository).findByListingStatusAndCategoryId(any(), any(), captor.capture());
		return captor.getValue();
	}

	@SuppressWarnings("unchecked")
	private Collection<Long> captureRequestedIds() {
		ArgumentCaptor<Collection<Long>> captor = ArgumentCaptor.forClass(Collection.class);
		verify(accommodationCardRepository).findByIdInAndListingStatus(captor.capture(), eq(ListingStatus.LISTED));
		return captor.getValue();
	}

	private static List<Long> ids(List<AccommodationSummaryResponse> cards) {
		return cards.stream().map(AccommodationSummaryResponse::id).toList();
	}

	private static AccommodationSummaryResponse card(Accommodation accommodation) {
		return new AccommodationSummaryResponse(accommodation.getId(), accommodation.getName(), null, null, null,
				null, null, null, null);
	}

	private static Accommodation accommodation(Long id) {
		Accommodation accommodation = Accommodation.create(100L, 1L, 1L, "숙소" + id, null, "주소", null, null,
				LocalTime.of(15, 0), LocalTime.of(11, 0), LocalDateTime.of(2026, 11, 1, 0, 0));
		ReflectionTestUtils.setField(accommodation, "id", id);
		accommodation.changeListingStatus(ListingStatus.LISTED);
		return accommodation;
	}

	private static AccommodationCategory category(Long id, String code, String name) {
		AccommodationCategory category = AccommodationCategory.of(name, id.intValue());
		ReflectionTestUtils.setField(category, "id", id);
		ReflectionTestUtils.setField(category, "code", code);
		return category;
	}
}
