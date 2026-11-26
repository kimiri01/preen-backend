package com.preen.preen_backend.accommodation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;
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

/**
 * 카테고리·지역·숙소 검색(AccommodationQueryService) 단위 테스트.
 * 검색 조건 검사(400 오류), 검색어 변환, 페이지 크기 보정, 목록 카드 정보 조합을 확인합니다.
 */
class AccommodationQueryServiceTest {

	private final CategoryRepository categoryRepository = mock(CategoryRepository.class);
	private final RegionRepository regionRepository = mock(RegionRepository.class);
	private final AccommodationRepository accommodationRepository = mock(AccommodationRepository.class);
	private final AccommodationImageRepository accommodationImageRepository = mock(AccommodationImageRepository.class);
	private final RoomPriceRepository roomPriceRepository = mock(RoomPriceRepository.class);

	private AccommodationQueryService service;

	@BeforeEach
	void setUp() {
		service = new AccommodationQueryService(categoryRepository, regionRepository, accommodationRepository,
				accommodationImageRepository, roomPriceRepository);
		// 검색 결과는 기본으로 "빈 페이지"를 돌려주게 해 둡니다. 필요한 테스트에서만 바꿉니다.
		when(accommodationRepository.search(any(), any(), any(), any(), any(), anyBoolean(), any(), any(),
				anyLong(), any())).thenAnswer(invocation -> new PageImpl<Accommodation>(List.of(),
						invocation.getArgument(9, Pageable.class), 0));
	}

	// ===== 카테고리·지역 목록 =====

	@Test
	@DisplayName("카테고리 목록을 정렬 순서 그대로 id·code·name으로 돌려준다")
	void getCategoriesReturnsSortedCategories() {
		AccommodationCategory hotel = category(1L, "HOTEL", "호텔·리조트");
		AccommodationCategory hanok = category(2L, "HANOK", "한옥");
		when(categoryRepository.findAllByOrderBySortOrderAsc()).thenReturn(List.of(hotel, hanok));

		List<CategoryResponse> result = service.getCategories();

		assertEquals(List.of(
				new CategoryResponse(1L, "HOTEL", "호텔·리조트"),
				new CategoryResponse(2L, "HANOK", "한옥")), result);
	}

	@Test
	@DisplayName("지역 목록은 시·도(level 1)만 조회한다")
	void getRegionsReturnsOnlyProvinces() {
		Region seoul = region(1L, "서울");
		when(regionRepository.findAllByLevelOrderByIdAsc(1)).thenReturn(List.of(seoul));

		List<RegionResponse> result = service.getRegions();

		assertEquals(1, result.size());
		assertEquals("서울", result.get(0).name());
		verify(regionRepository).findAllByLevelOrderByIdAsc(1);
	}

	// ===== 검색 조건 검사: 잘못된 값이면 400 오류 + DB 검색을 하지 않아야 한다 =====

	@Test
	@DisplayName("검색어가 100자를 넘으면 400 오류가 난다")
	void getAccommodationsRejectsTooLongKeyword() {
		String keyword = "가".repeat(101);

		assertBadRequest(() -> service.getAccommodations(keyword, null, null, null, null, null, 0, 8));
	}

	@Test
	@DisplayName("인원이 1명보다 적으면 400 오류가 난다")
	void getAccommodationsRejectsGuestCountBelowOne() {
		assertBadRequest(() -> service.getAccommodations(null, null, null, 0, null, null, 0, 8));
	}

	@Test
	@DisplayName("체크인·체크아웃 중 하나만 보내면 400 오류가 난다")
	void getAccommodationsRejectsHalfDateRange() {
		LocalDate date = LocalDate.of(2026, 12, 1);

		assertBadRequest(() -> service.getAccommodations(null, null, null, null, date, null, 0, 8));
		assertBadRequest(() -> service.getAccommodations(null, null, null, null, null, date, 0, 8));
	}

	@Test
	@DisplayName("체크아웃이 체크인과 같거나 앞이면 400 오류가 난다")
	void getAccommodationsRejectsCheckOutNotAfterCheckIn() {
		LocalDate checkIn = LocalDate.of(2026, 12, 1);

		assertBadRequest(() -> service.getAccommodations(null, null, null, null, checkIn, checkIn, 0, 8));
		assertBadRequest(() -> service.getAccommodations(null, null, null, null, checkIn, checkIn.minusDays(1), 0, 8));
	}

	// ===== 검색 조건을 Repository에 맞게 넘기는지 =====

	@Test
	@DisplayName("검색어는 앞뒤 공백을 지우고, LIKE 특수문자(%, _, !)를 이스케이프해서 %검색어% 로 넘긴다")
	void getAccommodationsEscapesKeyword() {
		service.getAccommodations("  50%_할인!  ", null, null, null, null, null, 0, 8);

		verify(accommodationRepository).search(eq(ListingStatus.LISTED), eq("%50!%!_할인!!%"), isNull(), isNull(),
				isNull(), eq(false), any(), any(), eq(0L), any());
	}

	@Test
	@DisplayName("검색어가 비어 있으면 숙소명 조건 없이(null) 검색한다")
	void getAccommodationsIgnoresBlankKeyword() {
		service.getAccommodations("   ", null, null, null, null, null, 0, 8);

		verify(accommodationRepository).search(eq(ListingStatus.LISTED), isNull(), isNull(), isNull(), isNull(),
				eq(false), any(), any(), eq(0L), any());
	}

	@Test
	@DisplayName("페이지는 0 미만이면 0으로, 크기는 1~50 사이로 보정하고 최근 등록순으로 정렬한다")
	void getAccommodationsClampsPageAndSize() {
		service.getAccommodations(null, null, null, null, null, null, -1, 100);

		Pageable pageable = capturePageable();
		assertEquals(0, pageable.getPageNumber());
		assertEquals(50, pageable.getPageSize());
		assertEquals(Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")), pageable.getSort());
	}

	@Test
	@DisplayName("페이지 크기가 0이면 1로 보정한다")
	void getAccommodationsClampsZeroSizeToOne() {
		service.getAccommodations(null, null, null, null, null, null, 0, 0);

		assertEquals(1, capturePageable().getPageSize());
	}

	@Test
	@DisplayName("날짜를 보내면 날짜 조건을 켜고 숙박 일수(박)를 계산해서 넘긴다")
	void getAccommodationsPassesDateFilterAndNights() {
		LocalDate checkIn = LocalDate.of(2026, 12, 1);
		LocalDate checkOut = LocalDate.of(2026, 12, 3);

		service.getAccommodations(null, 1L, 2L, 2, checkIn, checkOut, 0, 8);

		verify(accommodationRepository).search(eq(ListingStatus.LISTED), isNull(), eq(1L), eq(2L), eq(2), eq(true),
				eq(checkIn), eq(checkOut), eq(2L), any());
	}

	// ===== 검색 결과를 목록 카드 응답으로 바꾸는지 =====

	@Test
	@DisplayName("검색 결과에 카테고리명·지역명·대표 사진·최저 요금을 붙이고 페이지 정보를 그대로 돌려준다")
	void getAccommodationsBuildsSummaryPage() {
		// given: 숙소 2개가 검색되고, 전체 결과는 3개(다음 페이지 있음)다
		Accommodation first = accommodation(10L, 1L, 1L, "북촌 한옥스테이");
		Accommodation second = accommodation(11L, 2L, 1L, "해운대 호텔");
		Pageable pageable = PageRequest.of(0, 2);
		Page<Accommodation> page = new PageImpl<>(List.of(first, second), pageable, 3);
		// setUp에서 이미 동작을 정해 둔 메서드라서 when(...) 대신 doReturn(...)으로 바꿉니다.
		// (when(...)으로 다시 정하면 그 순간 기존 동작이 null 값으로 한 번 실행돼 오류가 납니다)
		doReturn(page).when(accommodationRepository).search(any(), any(), any(), any(), any(), anyBoolean(), any(),
				any(), anyLong(), any());
		stubSummaryData();

		// when
		PageResponse<AccommodationSummaryResponse> result =
				service.getAccommodations(null, null, null, null, null, null, 0, 2);

		// then: 카드 정보가 숙소마다 맞게 붙는다
		AccommodationSummaryResponse card = result.content().get(0);
		assertEquals(10L, card.id());
		assertEquals("북촌 한옥스테이", card.name());
		assertEquals("한옥", card.categoryName());
		assertEquals("서울", card.regionName());
		assertEquals("first-main.jpg", card.thumbnailUrl()); // 사진이 여러 장이면 첫 번째(대표) 사진
		assertEquals(120000, card.minNightlyPrice());

		AccommodationSummaryResponse secondCard = result.content().get(1);
		assertEquals("부산", secondCard.regionName());
		assertNull(secondCard.thumbnailUrl()); // 사진이 없으면 null
		assertNull(secondCard.minNightlyPrice()); // 객실 요금이 없으면 null

		// 페이지 정보
		assertEquals(0, result.page());
		assertEquals(2, result.size());
		assertEquals(3, result.totalElements());
		assertEquals(2, result.totalPages());
		assertTrue(result.hasNext());
	}

	@Test
	@DisplayName("검색 결과가 없으면 빈 목록을 돌려주고, 카드 정보용 추가 조회는 하지 않는다")
	void toSummariesSkipsQueriesWhenEmpty() {
		List<AccommodationSummaryResponse> result = service.toSummaries(List.of());

		assertTrue(result.isEmpty());
		verify(categoryRepository, never()).findAll();
		verifyNoInteractions(accommodationImageRepository, roomPriceRepository);
	}

	@Test
	@DisplayName("빈 검색 결과도 페이지 형식으로 돌려준다")
	void getAccommodationsReturnsEmptyPage() {
		PageResponse<AccommodationSummaryResponse> result =
				service.getAccommodations(null, null, null, null, null, null, 0, 8);

		assertTrue(result.content().isEmpty());
		assertEquals(0, result.totalElements());
		assertFalse(result.hasNext());
	}

	// ===== 테스트 도우미 =====

	/** 잘못된 조건이면 400 오류가 나고, DB 검색은 하지 않는지 확인합니다. */
	private void assertBadRequest(Runnable call) {
		ResponseStatusException exception = assertThrows(ResponseStatusException.class, call::run);
		assertEquals(400, exception.getStatusCode().value());
		verify(accommodationRepository, never()).search(any(), any(), any(), any(), any(), anyBoolean(), any(),
				any(), anyLong(), any());
	}

	/** search()에 넘어간 페이지 정보(Pageable)를 꺼냅니다. */
	private Pageable capturePageable() {
		ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
		verify(accommodationRepository).search(any(), any(), any(), any(), any(), anyBoolean(), any(), any(),
				anyLong(), captor.capture());
		return captor.getValue();
	}

	/** 카드에 붙일 카테고리·지역·사진·최저 요금 데이터를 준비합니다. */
	private void stubSummaryData() {
		when(categoryRepository.findAll()).thenReturn(List.of(category(1L, "HANOK", "한옥")));
		when(regionRepository.findAll()).thenReturn(List.of(region(1L, "서울"), region(2L, "부산")));
		when(accommodationImageRepository.findAllByAccommodationIdInOrderBySortOrderAsc(List.of(10L, 11L)))
				.thenReturn(List.of(
						AccommodationImage.of(10L, "first-main.jpg", 0),
						AccommodationImage.of(10L, "first-room.jpg", 1)));
		when(roomPriceRepository.findMinNightlyPrices(List.of(10L, 11L)))
				.thenReturn(List.of(minPrice(10L, "120000")));
	}

	private static Accommodation accommodation(Long id, Long regionId, Long categoryId, String name) {
		Accommodation accommodation = Accommodation.create(100L, regionId, categoryId, name, null, "주소",
				null, null, LocalTime.of(15, 0), LocalTime.of(11, 0), LocalDateTime.of(2026, 11, 1, 0, 0));
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

	private static Region region(Long id, String name) {
		Region region = Region.of(null, name, 1);
		ReflectionTestUtils.setField(region, "id", id);
		return region;
	}

	private static RoomPriceRepository.MinPrice minPrice(Long accommodationId, String price) {
		return new RoomPriceRepository.MinPrice() {
			@Override
			public Long getAccommodationId() {
				return accommodationId;
			}

			@Override
			public BigDecimal getMinNightlyPrice() {
				return new BigDecimal(price);
			}
		};
	}
}
