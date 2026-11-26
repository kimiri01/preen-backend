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
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.preen.preen_backend.accommodation.domain.Accommodation;
import com.preen.preen_backend.accommodation.domain.Accommodation.ListingStatus;
import com.preen.preen_backend.accommodation.domain.AccommodationAmenity;
import com.preen.preen_backend.accommodation.domain.AccommodationImage;
import com.preen.preen_backend.accommodation.domain.Amenity;
import com.preen.preen_backend.accommodation.dto.HostAccommodationCreateRequest;
import com.preen.preen_backend.accommodation.dto.HostAccommodationCreateResponse;
import com.preen.preen_backend.accommodation.repository.AccommodationAmenityRepository;
import com.preen.preen_backend.accommodation.repository.AccommodationImageRepository;
import com.preen.preen_backend.accommodation.repository.AccommodationRepository;
import com.preen.preen_backend.accommodation.repository.AmenityRepository;
import com.preen.preen_backend.accommodation.repository.CategoryRepository;
import com.preen.preen_backend.accommodation.repository.HostAccountRepository;
import com.preen.preen_backend.accommodation.repository.RegionRepository;

/**
 * 호스트 숙소 등록(HostAccommodationCommandService) 단위 테스트.
 * 권한 검사(403), 입력값 검사(400), 저장되는 값(공백 정리·DRAFT 상태·사진 순서·편의시설 중복 제거)을 확인합니다.
 */
class HostAccommodationCommandServiceTest {

	private static final Long HOST_USER_ID = 7L;
	private static final Long HOST_BUSINESS_ID = 70L;
	private static final Long NEW_ACCOMMODATION_ID = 500L;

	private final HostAccountRepository hostAccountRepository = mock(HostAccountRepository.class);
	private final AccommodationRepository accommodationRepository = mock(AccommodationRepository.class);
	private final AccommodationImageRepository accommodationImageRepository = mock(AccommodationImageRepository.class);
	private final AccommodationAmenityRepository accommodationAmenityRepository =
			mock(AccommodationAmenityRepository.class);
	private final AmenityRepository amenityRepository = mock(AmenityRepository.class);
	private final RegionRepository regionRepository = mock(RegionRepository.class);
	private final CategoryRepository categoryRepository = mock(CategoryRepository.class);
	// 현재 시각을 고정해서, 저장되는 등록 시각을 정확히 비교할 수 있게 합니다.
	private final Clock clock = Clock.fixed(Instant.parse("2026-11-27T01:00:00Z"), ZoneId.of("Asia/Seoul"));

	private HostAccommodationCommandService service;

	@BeforeEach
	void setUp() {
		service = new HostAccommodationCommandService(hostAccountRepository, accommodationRepository,
				accommodationImageRepository, accommodationAmenityRepository, amenityRepository, regionRepository,
				categoryRepository, clock);

		// 기본 상태: 승인된 사업자가 있는 HOST 회원이고, 지역·카테고리·편의시설이 모두 존재한다.
		// 실패 테스트에서는 필요한 것만 하나씩 바꿉니다.
		when(hostAccountRepository.countActiveHost(HOST_USER_ID)).thenReturn(1L);
		when(hostAccountRepository.findApprovedHostBusinessId(HOST_USER_ID)).thenReturn(Optional.of(HOST_BUSINESS_ID));
		when(regionRepository.existsById(1L)).thenReturn(true);
		when(categoryRepository.existsById(2L)).thenReturn(true);
		when(amenityRepository.findAllById(any())).thenReturn(List.of(amenity(), amenity()));
		// save는 DB처럼 id를 채워서 돌려주게 합니다.
		when(accommodationRepository.save(any(Accommodation.class))).thenAnswer(invocation -> {
			Accommodation saved = invocation.getArgument(0);
			ReflectionTestUtils.setField(saved, "id", NEW_ACCOMMODATION_ID);
			return saved;
		});
	}

	// ===== 권한 검사: 403 =====

	@Test
	@DisplayName("활성 HOST 회원이 아니면 403 오류가 나고 아무것도 저장하지 않는다")
	void createRejectsNonHost() {
		when(hostAccountRepository.countActiveHost(HOST_USER_ID)).thenReturn(0L);

		assertStatus(403, () -> service.create(HOST_USER_ID, request()));
		verifyNothingSaved();
	}

	@Test
	@DisplayName("승인된 사업자 정보가 없으면 403 오류가 난다")
	void createRejectsHostWithoutApprovedBusiness() {
		when(hostAccountRepository.findApprovedHostBusinessId(HOST_USER_ID)).thenReturn(Optional.empty());

		assertStatus(403, () -> service.create(HOST_USER_ID, request()));
		verifyNothingSaved();
	}

	// ===== 입력값 검사: 400 =====

	@Test
	@DisplayName("없는 지역이면 400 오류가 난다")
	void createRejectsUnknownRegion() {
		when(regionRepository.existsById(1L)).thenReturn(false);

		assertStatus(400, () -> service.create(HOST_USER_ID, request()));
		verifyNothingSaved();
	}

	@Test
	@DisplayName("없는 카테고리면 400 오류가 난다")
	void createRejectsUnknownCategory() {
		when(categoryRepository.existsById(2L)).thenReturn(false);

		assertStatus(400, () -> service.create(HOST_USER_ID, request()));
		verifyNothingSaved();
	}

	@Test
	@DisplayName("없는 편의시설이 섞여 있으면 400 오류가 난다")
	void createRejectsUnknownAmenity() {
		// 편의시설 2개를 보냈는데 DB에는 1개만 있다
		when(amenityRepository.findAllById(any())).thenReturn(List.of(amenity()));

		assertStatus(400, () -> service.create(HOST_USER_ID, request()));
		verifyNothingSaved();
	}

	// ===== 정상 등록 =====

	@Test
	@DisplayName("숙소를 작성 중(DRAFT) 상태로 저장하고, 이름·주소·소개의 앞뒤 공백을 지운다")
	void createSavesDraftAccommodationWithTrimmedValues() {
		// when
		HostAccommodationCreateResponse response = service.create(HOST_USER_ID, request());

		// then: 저장된 숙소 값 확인
		ArgumentCaptor<Accommodation> captor = ArgumentCaptor.forClass(Accommodation.class);
		verify(accommodationRepository).save(captor.capture());
		Accommodation saved = captor.getValue();
		assertEquals(HOST_BUSINESS_ID, saved.getHostBusinessId()); // 회원 ID가 아니라 승인된 사업자 ID로 저장
		assertEquals("북촌 한옥스테이", saved.getName());
		assertEquals("서울 종로구 계동길 1", saved.getAddress());
		assertEquals("조용한 한옥입니다.\n마당이 있어요.", saved.getDescription()); // 줄바꿈은 유지
		assertEquals(ListingStatus.DRAFT, saved.getListingStatus());
		assertEquals(LocalDateTime.now(clock), saved.getCreatedAt());

		// 응답 확인
		assertEquals(new HostAccommodationCreateResponse(NEW_ACCOMMODATION_ID, "북촌 한옥스테이", "DRAFT"), response);
	}

	@Test
	@DisplayName("사진은 보낸 순서대로 0, 1, 2… 순번을 붙여 저장한다 (0번이 대표 사진)")
	void createSavesImagesInRequestOrder() {
		service.create(HOST_USER_ID, request());

		List<AccommodationImage> images = captureSavedImages();
		assertEquals(2, images.size());
		assertEquals("main.jpg", images.get(0).getImageUrl()); // 앞뒤 공백 제거
		assertEquals(0, images.get(0).getSortOrder());
		assertEquals("room.jpg", images.get(1).getImageUrl());
		assertEquals(1, images.get(1).getSortOrder());
		assertTrue(images.stream().allMatch(image -> image.getAccommodationId().equals(NEW_ACCOMMODATION_ID)));
	}

	@Test
	@DisplayName("편의시설 ID가 중복되면 한 번만 연결한다")
	void createLinksDistinctAmenities() {
		service.create(HOST_USER_ID, request()); // 요청 편의시설: [3, 4, 3]

		List<AccommodationAmenity> links = captureSavedAmenities();
		assertEquals(List.of(3L, 4L), links.stream().map(AccommodationAmenity::getAmenityId).toList());
		assertTrue(links.stream().allMatch(link -> link.getAccommodationId().equals(NEW_ACCOMMODATION_ID)));
	}

	@Test
	@DisplayName("소개가 공백뿐이면 null로, 사진·편의시설이 없으면 빈 목록으로 저장한다")
	void createHandlesOptionalFieldsMissing() {
		HostAccommodationCreateRequest request = new HostAccommodationCreateRequest(1L, 2L, "북촌 한옥스테이",
				"   ", "서울 종로구 계동길 1", null, null, LocalTime.of(15, 0), LocalTime.of(11, 0), null, null);

		service.create(HOST_USER_ID, request);

		ArgumentCaptor<Accommodation> captor = ArgumentCaptor.forClass(Accommodation.class);
		verify(accommodationRepository).save(captor.capture());
		assertNull(captor.getValue().getDescription());
		assertTrue(captureSavedImages().isEmpty());
		assertTrue(captureSavedAmenities().isEmpty());
		// 편의시설을 안 보냈으면 존재 여부도 확인하지 않는다
		verify(amenityRepository, never()).findAllById(any());
	}

	// ===== 테스트 도우미 =====

	/** 공백이 섞인 정상 요청 (편의시설 3번이 중복) */
	private static HostAccommodationCreateRequest request() {
		return new HostAccommodationCreateRequest(
				1L,
				2L,
				"  북촌 한옥스테이  ",
				"\n 조용한 한옥입니다.\n마당이 있어요. \n",
				"  서울 종로구 계동길 1 ",
				new BigDecimal("37.5826"),
				new BigDecimal("126.9860"),
				LocalTime.of(15, 0),
				LocalTime.of(11, 0),
				List.of(" main.jpg ", "room.jpg"),
				List.of(3L, 4L, 3L));
	}

	private static void assertStatus(int status, Runnable call) {
		ResponseStatusException exception = assertThrows(ResponseStatusException.class, call::run);
		assertEquals(status, exception.getStatusCode().value());
	}

	private void verifyNothingSaved() {
		verify(accommodationRepository, never()).save(any());
		verify(accommodationImageRepository, never()).saveAll(any());
		verify(accommodationAmenityRepository, never()).saveAll(any());
	}

	@SuppressWarnings("unchecked")
	private List<AccommodationImage> captureSavedImages() {
		ArgumentCaptor<List<AccommodationImage>> captor = ArgumentCaptor.forClass(List.class);
		verify(accommodationImageRepository).saveAll(captor.capture());
		return captor.getValue();
	}

	@SuppressWarnings("unchecked")
	private List<AccommodationAmenity> captureSavedAmenities() {
		ArgumentCaptor<List<AccommodationAmenity>> captor = ArgumentCaptor.forClass(List.class);
		verify(accommodationAmenityRepository).saveAll(captor.capture());
		return captor.getValue();
	}

	private static Amenity amenity() {
		return BeanUtils.instantiateClass(Amenity.class);
	}
}
