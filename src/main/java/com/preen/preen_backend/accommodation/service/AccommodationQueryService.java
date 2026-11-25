package com.preen.preen_backend.accommodation.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.preen.preen_backend.accommodation.domain.*;
import com.preen.preen_backend.accommodation.dto.*;
import com.preen.preen_backend.accommodation.dto.RefundDtos.*;
import com.preen.preen_backend.accommodation.exception.AccommodationErrorCode;
import com.preen.preen_backend.accommodation.repository.*;
import com.preen.preen_backend.common.exception.BusinessException;
import com.preen.preen_backend.host.service.HostService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
import com.preen.preen_backend.accommodation.dto.HostAccommodationDtos.*;
import lombok.RequiredArgsConstructor;

/** 카테고리·지역·숙소 "조회" 전용 서비스 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccommodationQueryService {

	private static final int MAX_PAGE_SIZE = 50;
	private static final int MAX_KEYWORD_LENGTH = 100;
	/**
	 * 시·도 단계 지역 (domain2 Region.level)
	 */
	private static final int LEVEL_PROVINCE = 1;

	private final CategoryRepository categoryRepository;
	private final RegionRepository regionRepository;
	private final AccommodationRepository accommodationRepository;
	private final AccommodationImageRepository accommodationImageRepository;
	private final AccommodationAmenityRepository accommodationAmenityRepository;
	private final RoomRepository roomRepository;
	private final RoomImageRepository roomImageRepository;
	private final AmenityRepository amenityRepository;
	private final RefundPolicyRepository refundPolicyRepository;
	private final RefundPolicyRuleRepository refundPolicyRuleRepository;
	private final RoomPriceRepository roomPriceRepository;
	private final HostService hostService;

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

	/**
	 * 공개(LISTED) 숙소를 최근 등록순으로 조회합니다.
	 * keyword, categoryId, regionId, guestCount는 없으면(null) 해당 조건을 적용하지 않습니다.
	 * keyword는 숙소명에 그 글자가 들어 있는 숙소만 찾습니다. (대소문자 구분 없음, 공백뿐이면 조건 없음)
	 * guestCount는 최대 인원이 그 이상인 판매 중 객실이 있는 숙소만 찾습니다.
	 */
	public PageResponse<AccommodationSummaryResponse> getAccommodations(String keyword, Long categoryId,
																		Long regionId, Integer guestCount, LocalDate checkIn, LocalDate checkOut, int page, int size) {
		if (keyword != null && keyword.length() > MAX_KEYWORD_LENGTH) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"검색어는 " + MAX_KEYWORD_LENGTH + "자 이하여야 합니다.");
		}
		if (guestCount != null && guestCount < 1) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "인원은 1명 이상이어야 합니다.");
		}
		// 잘못된 값이 와도 에러 대신 허용 범위로 보정합니다. (page 0 이상, size 1~50)
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

	/**
	 * 검색어를 "포함" LIKE 패턴으로 바꿉니다. 비어 있으면 null(조건 없음)입니다.
	 * 검색어 안의 %, _는 글자 그대로 찾도록 '!'로 이스케이프합니다.
	 */
	private static String toNamePattern(String keyword) {
		if (keyword == null || keyword.isBlank()) {
			return null;
		}
		String escaped = keyword.trim().replace("!", "!!").replace("%", "!%").replace("_", "!_");
		return "%" + escaped + "%";
	}

	/**
	 * 숙소 목록에 카테고리명·지역명·대표 사진을 붙입니다.
	 * 연관관계가 없으므로 id 묶음으로 한 번씩만 조회합니다. (N+1 방지)
	 */
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
		// [kny] 카드의 "최저 1박 요금" = 판매 중(ACTIVE) 객실의 기본가 중 가장 낮은 값 (STAY-001)
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
				minPrices.get(a.getId())); // 판매 중인 객실이 없으면 null
		return accommodations.stream().map(mapper).toList();
	}


	/** 호스트의 내 숙소 목록. 공개 상태와 관계없이 모두, 최근 등록순. */

	public List<HostAccommodationSummary> getHostSummaries(Long hostBusinessId) {
		List<Accommodation> accommodations =
				accommodationRepository.findByHostBusinessIdOrderByIdDesc(hostBusinessId);
		if (accommodations.isEmpty()) {
			return List.of();
		}

		List<Long> ids = accommodations.stream().map(Accommodation::getId).toList();
		Map<Long, AccommodationSummaryResponse> summaries = toSummaries(accommodations).stream()
				.collect(Collectors.toMap(AccommodationSummaryResponse::id, Function.identity()));
		Map<Long, List<Room>> roomsByAccommodation = roomRepository.findByAccommodationIdIn(ids).stream()
				.collect(Collectors.groupingBy(Room::getAccommodationId));
		Set<Long> withPolicy = refundPolicyRepository.findByAccommodationIdInAndActiveTrue(ids).stream()
				.map(RefundPolicy::getAccommodationId)
				.collect(Collectors.toSet());

		return accommodations.stream()
				.map(a -> toHostSummary(
						a,
						summaries.get(a.getId()),
						roomsByAccommodation.getOrDefault(a.getId(), List.of()),
						withPolicy.contains(a.getId())))
				.toList();
	}

	private HostAccommodationSummary toHostSummary(Accommodation a, AccommodationSummaryResponse r,
												   List<Room> rooms, boolean hasPolicy) {
		int activeRooms = (int) rooms.stream().filter(Room::isActive).count();
		boolean publishable = activeRooms > 0 && hasPolicy;

		return new HostAccommodationSummary(
				a.getId(), a.getName(), a.getAddress(),
				a.getRegionId(), r.regionName(),
				a.getCategoryId(), r.categoryName(),
				r.thumbnailUrl(), a.getListingStatus(),
				rooms.size(), activeRooms, hasPolicy, publishable);
	}



	public HostAccommodationDetail toHostDetail(Accommodation accommodation, boolean activeRoomsOnly) {
		Long id = accommodation.getId();
		List<String> imageUrls = accommodationImageRepository.findByAccommodationIdOrderBySortOrderAsc(id)
				.stream()
				.map(AccommodationImage::getImageUrl)
				.toList();

		List<Long> amenityIds = accommodationAmenityRepository.findByAccommodationId(id)
				.stream()
				.map(AccommodationAmenity::getAmenityId).toList();
		List<AmenityResponse> amenities = amenityRepository.findAllById(amenityIds)
				.stream()
				.sorted(Comparator.comparing(Amenity::getId))
				.map(a -> new AmenityResponse(a.getId(), a.getName()))
				.toList();
		List<Room> rooms = roomRepository.findByAccommodationIdOrderByIdAsc(id)
				.stream()
				.filter(r -> !activeRoomsOnly || r.isActive() )
				.toList();

		Map<Long, List<String>> roomImages = rooms.isEmpty() ? Map.of()
				: roomImageRepository.findByRoomIdInOrderBySortOrderAsc(
						rooms.stream().map(Room::getId).toList())
				.stream()
				.collect(Collectors.groupingBy(RoomImage::getRoomId,
						Collectors.mapping(RoomImage::getImageUrl, Collectors.toList())));
		return new HostAccommodationDetail(

				id, accommodation.getName(), accommodation.getDescription(), accommodation.getAddress(),
				accommodation.getRegionId(),
				regionRepository.findById(accommodation.getRegionId()).map(Region::getName).orElse(null),
				accommodation.getCategoryId(),
				categoryRepository.findById(accommodation.getCategoryId()).map(AccommodationCategory::getName)
						.orElse(null),
				accommodation.getLatitude(), accommodation.getLongitude(),
				accommodation.getCheckInTime(), accommodation.getCheckOutTime(),
				accommodation.getListingStatus(), imageUrls, amenities,
				rooms.stream().map(r -> toRoomResponse(r, roomImages.getOrDefault(r.getId(), List.of()))).toList(),
				getActiveRefundPolicy(id).orElse(null));

	}


	public RoomResponse toRoomResponse(Room room, List<String> imageUrls) {
		return new RoomResponse(room.getId(), room.getName(), room.getCapacity(), room.getMaxCapacity(),
				room.getExtraPersonFee(), room.getNightlyPrice(), room.getStatus(), imageUrls);
	}




//=========================================
// ---------- 다른 feature용 ----------

	public RoomInfo getRoomInfo(Long roomId) {
		Room room = roomRepository.findById(roomId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "객실을 찾을 수 없습니다."));

		Accommodation accommodation = accommodationRepository.findById(room.getAccommodationId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "숙소를 찾을 수 없습니다."));

		return toRoomInfo(room, accommodation);
	}


	private RoomInfo toRoomInfo(Room room, Accommodation accommodation) {
		return new RoomInfo(room.getId(), accommodation.getId(), accommodation.getName(), room.getName(),
				room.getCapacity(), room.getMaxCapacity(), room.getExtraPersonFee(), room.getNightlyPrice(),
				room.isActive() && accommodation.isListed());
	}



	/** 현재 적용 중인 환불정책과 규칙 (일수 내림차순). */
	public Optional<RefundPolicyResponse> getActiveRefundPolicy(Long accommodationId) {
		return refundPolicyRepository.findFirstByAccommodationIdAndActiveTrueOrderByIdDesc(accommodationId)
				.map(this::toPolicyResponse);
	}


	public RefundPolicyResponse toPolicyResponse(RefundPolicy policy) {
		List<RefundRule> rules = refundPolicyRuleRepository
				.findByRefundPolicyIdOrderByDaysBeforeDesc(policy.getId()).stream()
				.map(r -> new RefundRule(r.getDaysBefore(), r.getRefundRate()))
				.toList();
		return new RefundPolicyResponse(policy.getId(), policy.getVersion(), policy.getPolicyText(), rules);
	}



	/** 객실이 그 회원(호스트)의 것인지 검사한다. 아니면 예외. */
	public void assertRoomOwner(Long roomId, Long userId) {
		Room room = roomRepository.findById(roomId)
				.orElseThrow(() -> new BusinessException(AccommodationErrorCode.ROOM_NOT_FOUND));
		assertAccommodationOwner(room.getAccommodationId(), userId);
	}

	public void assertAccommodationOwner(Long accommodationId, Long userId) {
		Accommodation accommodation = accommodationRepository.findById(accommodationId)
				.orElseThrow(() -> new BusinessException(AccommodationErrorCode.ACCOMMODATION_NOT_FOUND));
		if (!hostService.getBusinessOf(userId).businessId().equals(accommodation.getHostBusinessId())) {
			throw new BusinessException(AccommodationErrorCode.NOT_OWNER);
		}
	}


	/** 판매 중(ACTIVE)인 모든 객실. 재고 자동 생성 스케줄러가 쓴다. */
	public List<RoomInfo> getAllActiveRooms() {
		List<Room> rooms = roomRepository.findByStatus(Room.RoomStatus.ACTIVE);
		Map<Long, Accommodation> accommodations = accommodationRepository
				.findAllById(rooms.stream().map(Room::getAccommodationId).distinct().toList()).stream()
				.collect(Collectors.toMap(Accommodation::getId, Function.identity()));
		return rooms.stream()
				.filter(r -> accommodations.containsKey(r.getAccommodationId()))
				.map(r -> toRoomInfo(r, accommodations.get(r.getAccommodationId())))
				.toList();
	}

	/**
	 * 호스트의 객실 ID 목록. accommodationId를 주면 그 숙소의 객실만 돌려주고, 본인 숙소가 아니면 예외.
	 */
	public List<Long> getRoomIdsOfHost(Long userId, Long accommodationId) {
		List<Long> accommodationIds;
		if (accommodationId != null) {
			assertAccommodationOwner(accommodationId, userId);        // 이미 있는 메서드
			accommodationIds = List.of(accommodationId);
		} else {
			Long businessId = hostService.getBusinessOf(userId).businessId();
			accommodationIds = accommodationRepository.findByHostBusinessIdOrderByIdDesc(businessId).stream()
					.map(Accommodation::getId).toList();
		}
		if (accommodationIds.isEmpty()) {
			return List.of();
		}
		return roomRepository.findByAccommodationIdIn(accommodationIds).stream()
				.map(Room::getId).toList();
	}

	public Map<Long, RoomInfo> getRoomInfos(Collection<Long> roomIds) {
		List<Room> rooms = roomRepository.findAllById(roomIds);
		Map<Long, Accommodation> accommodations = accommodationRepository
				.findAllById(rooms.stream().map(Room::getAccommodationId).distinct().toList()).stream()
				.collect(Collectors.toMap(Accommodation::getId, Function.identity()));
		return rooms.stream().collect(Collectors.toMap(Room::getId,
				r -> toRoomInfo(r, accommodations.get(r.getAccommodationId()))));
	}
}
