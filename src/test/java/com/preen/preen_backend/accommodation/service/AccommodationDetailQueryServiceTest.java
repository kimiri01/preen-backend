package com.preen.preen_backend.accommodation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.preen.preen_backend.accommodation.domain.Accommodation;
import com.preen.preen_backend.accommodation.domain.Accommodation.ListingStatus;
import com.preen.preen_backend.accommodation.domain.AccommodationCategory;
import com.preen.preen_backend.accommodation.domain.AccommodationImage;
import com.preen.preen_backend.accommodation.domain.Amenity;
import com.preen.preen_backend.accommodation.domain.Region;
import com.preen.preen_backend.accommodation.dto.AccommodationDetailResponse;
import com.preen.preen_backend.accommodation.repository.AccommodationImageRepository;
import com.preen.preen_backend.accommodation.repository.AccommodationRepository;
import com.preen.preen_backend.accommodation.repository.AmenityRepository;
import com.preen.preen_backend.accommodation.repository.CategoryRepository;
import com.preen.preen_backend.accommodation.repository.RegionRepository;

/**
 * 숙소 상세 조회(AccommodationDetailQueryService) 단위 테스트.
 * DB 없이 Repository를 가짜(mock)로 바꿔서, 서비스가 "받은 데이터로 응답을 맞게 만드는지"만 검사합니다.
 */
class AccommodationDetailQueryServiceTest {

	private static final Long ACCOMMODATION_ID = 10L;
	private static final Long CATEGORY_ID = 1L;
	private static final Long REGION_ID = 2L;

	// 가짜 Repository: 진짜 DB 대신 "이 메서드를 부르면 이 값을 돌려줘"라고 미리 정해 둡니다.
	private final AccommodationRepository accommodationRepository = mock(AccommodationRepository.class);
	private final CategoryRepository categoryRepository = mock(CategoryRepository.class);
	private final RegionRepository regionRepository = mock(RegionRepository.class);
	private final AccommodationImageRepository accommodationImageRepository = mock(AccommodationImageRepository.class);
	private final AmenityRepository amenityRepository = mock(AmenityRepository.class);

	private AccommodationDetailQueryService service;

	@BeforeEach
	void setUp() {
		service = new AccommodationDetailQueryService(accommodationRepository, categoryRepository,
				regionRepository, accommodationImageRepository, amenityRepository);
	}

	@Test
	@DisplayName("공개(LISTED) 숙소를 조회하면 카테고리명·지역명·사진·편의시설을 담아 돌려준다")
	void getDetailReturnsListedAccommodation() {
		// given: 공개 숙소 1개와 그 숙소의 카테고리·지역·사진·편의시설이 있다
		Accommodation accommodation = accommodation(ListingStatus.LISTED);
		when(accommodationRepository.findById(ACCOMMODATION_ID)).thenReturn(Optional.of(accommodation));
		when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category(CATEGORY_ID, "한옥")));
		when(regionRepository.findById(REGION_ID)).thenReturn(Optional.of(region(REGION_ID, "서울")));
		when(accommodationImageRepository.findAllByAccommodationIdInOrderBySortOrderAsc(List.of(ACCOMMODATION_ID)))
				.thenReturn(List.of(
						AccommodationImage.of(ACCOMMODATION_ID, "main.jpg", 0),
						AccommodationImage.of(ACCOMMODATION_ID, "room.jpg", 1)));
		when(amenityRepository.findAllByAccommodationId(ACCOMMODATION_ID))
				.thenReturn(List.of(amenity("와이파이"), amenity("주차")));

		// when: 상세 조회를 하면
		AccommodationDetailResponse response = service.getDetail(ACCOMMODATION_ID);

		// then: 숙소 정보와 이름들이 응답에 그대로 담긴다
		assertEquals(ACCOMMODATION_ID, response.id());
		assertEquals("북촌 한옥스테이", response.name());
		assertEquals("조용한 한옥 숙소입니다.", response.description());
		assertEquals("서울 종로구 계동길 1", response.address());
		assertEquals(REGION_ID, response.regionId());
		assertEquals("서울", response.regionName());
		assertEquals(CATEGORY_ID, response.categoryId());
		assertEquals("한옥", response.categoryName());
		assertEquals(List.of("main.jpg", "room.jpg"), response.imageUrls()); // 정렬 순서 유지 (첫 번째가 대표 사진)
		assertEquals(List.of("와이파이", "주차"), response.amenities());
	}

	@Test
	@DisplayName("없는 숙소를 조회하면 404 오류가 난다")
	void getDetailThrowsNotFoundWhenMissing() {
		// given: DB에 해당 숙소가 없다
		when(accommodationRepository.findById(ACCOMMODATION_ID)).thenReturn(Optional.empty());

		// when & then: 조회하면 404 오류가 난다
		ResponseStatusException exception = assertThrows(ResponseStatusException.class,
				() -> service.getDetail(ACCOMMODATION_ID));
		assertEquals(404, exception.getStatusCode().value());
	}

	@Test
	@DisplayName("숨김(HIDDEN)·작성 중(DRAFT) 숙소는 있어도 404로 처리하고, 다른 정보는 조회하지 않는다")
	void getDetailThrowsNotFoundWhenNotListed() {
		for (ListingStatus status : List.of(ListingStatus.HIDDEN, ListingStatus.DRAFT)) {
			// given: 숙소는 있지만 공개 상태가 아니다
			when(accommodationRepository.findById(ACCOMMODATION_ID)).thenReturn(Optional.of(accommodation(status)));

			// when & then: 404 오류가 난다
			ResponseStatusException exception = assertThrows(ResponseStatusException.class,
					() -> service.getDetail(ACCOMMODATION_ID));
			assertEquals(404, exception.getStatusCode().value());
		}
		// 공개 숙소가 아니면 카테고리·사진 같은 추가 조회를 하지 않아야 한다
		verify(categoryRepository, never()).findById(any());
		verify(accommodationImageRepository, never()).findAllByAccommodationIdInOrderBySortOrderAsc(any());
	}

	@Test
	@DisplayName("카테고리·지역·사진·편의시설이 없으면 이름은 null, 목록은 빈 배열로 돌려준다")
	void getDetailHandlesMissingRelatedData() {
		// given: 숙소만 있고 연결된 정보는 하나도 없다
		when(accommodationRepository.findById(ACCOMMODATION_ID))
				.thenReturn(Optional.of(accommodation(ListingStatus.LISTED)));
		when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.empty());
		when(regionRepository.findById(REGION_ID)).thenReturn(Optional.empty());
		when(accommodationImageRepository.findAllByAccommodationIdInOrderBySortOrderAsc(List.of(ACCOMMODATION_ID)))
				.thenReturn(List.of());
		when(amenityRepository.findAllByAccommodationId(ACCOMMODATION_ID)).thenReturn(List.of());

		// when
		AccommodationDetailResponse response = service.getDetail(ACCOMMODATION_ID);

		// then: 오류 없이 빈 값으로 응답한다 (프론트가 null·빈 배열을 처리)
		assertNull(response.categoryName());
		assertNull(response.regionName());
		assertTrue(response.imageUrls().isEmpty());
		assertTrue(response.amenities().isEmpty());
	}

	// ===== 테스트용 데이터 만들기 =====

	private static Accommodation accommodation(ListingStatus status) {
		Accommodation accommodation = Accommodation.create(100L, REGION_ID, CATEGORY_ID, "북촌 한옥스테이",
				"조용한 한옥 숙소입니다.", "서울 종로구 계동길 1", BigDecimal.ZERO, BigDecimal.ZERO,
				LocalTime.of(15, 0), LocalTime.of(11, 0), LocalDateTime.of(2026, 11, 1, 0, 0));
		ReflectionTestUtils.setField(accommodation, "id", ACCOMMODATION_ID); // id는 DB가 만들기 때문에 직접 넣어 줍니다
		accommodation.changeListingStatus(status);
		return accommodation;
	}

	private static AccommodationCategory category(Long id, String name) {
		AccommodationCategory category = AccommodationCategory.of(name, 1);
		ReflectionTestUtils.setField(category, "id", id);
		return category;
	}

	private static Region region(Long id, String name) {
		Region region = Region.of(null, name, 1);
		ReflectionTestUtils.setField(region, "id", id);
		return region;
	}

	private static Amenity amenity(String name) {
		// Amenity는 만드는 메서드가 없어서, 빈 객체를 만든 뒤 이름만 넣습니다
		Amenity amenity = BeanUtils.instantiateClass(Amenity.class);
		ReflectionTestUtils.setField(amenity, "name", name);
		return amenity;
	}
}
