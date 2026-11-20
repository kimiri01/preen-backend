package com.preen.preen_backend.accommodation.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.preen.preen_backend.accommodation.domain.Accommodation;
import com.preen.preen_backend.accommodation.domain.AccommodationAmenity;
import com.preen.preen_backend.accommodation.domain.AccommodationImage;
import com.preen.preen_backend.accommodation.dto.HostAccommodationCreateRequest;
import com.preen.preen_backend.accommodation.dto.HostAccommodationCreateResponse;
import com.preen.preen_backend.accommodation.repository.AccommodationAmenityRepository;
import com.preen.preen_backend.accommodation.repository.AccommodationImageRepository;
import com.preen.preen_backend.accommodation.repository.AccommodationRepository;
import com.preen.preen_backend.accommodation.repository.AmenityRepository;
import com.preen.preen_backend.accommodation.repository.CategoryRepository;
import com.preen.preen_backend.accommodation.repository.HostAccountRepository;
import com.preen.preen_backend.accommodation.repository.RegionRepository;

import lombok.RequiredArgsConstructor;

/**
 * [kny] 호스트 숙소 등록 (기능명세서 HOST-002 중 "등록")
 * 호스트가 입력한 숙소 소개(description)가 그대로 저장되어, 공개 후 상세페이지(STAY-004)에 보입니다.
 */
@Service
@RequiredArgsConstructor
public class HostAccommodationCommandService {

	private final HostAccountRepository hostAccountRepository;
	private final AccommodationRepository accommodationRepository;
	private final AccommodationImageRepository accommodationImageRepository;
	private final AccommodationAmenityRepository accommodationAmenityRepository;
	private final AmenityRepository amenityRepository;
	private final RegionRepository regionRepository;
	private final CategoryRepository categoryRepository;
	private final Clock clock;

	/**
	 * 숙소를 작성 중(DRAFT) 상태로 등록합니다. 사진과 편의시설도 함께 저장하고, 중간에 실패하면 전부 취소됩니다.
	 * - HOST 회원이 아니면 403
	 * - 승인된 사업자가 없으면 403
	 * - 없는 지역·카테고리·편의시설이면 400
	 */
	@Transactional
	public HostAccommodationCreateResponse create(Long hostUserId, HostAccommodationCreateRequest request) {
		if (hostAccountRepository.countActiveHost(hostUserId) == 0) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
		}
		Long hostBusinessId = hostAccountRepository.findApprovedHostBusinessId(hostUserId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
						"승인된 사업자 정보가 없습니다. 사업자 등록 승인 후 숙소를 등록할 수 있습니다."));

		if (!regionRepository.existsById(request.regionId())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "존재하지 않는 지역입니다.");
		}
		if (!categoryRepository.existsById(request.categoryId())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "존재하지 않는 카테고리입니다.");
		}
		List<Long> amenityIds = request.amenityIds() == null
				? List.of()
				: request.amenityIds().stream().distinct().toList();
		if (!amenityIds.isEmpty() && amenityRepository.findAllById(amenityIds).size() != amenityIds.size()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "존재하지 않는 편의시설이 포함되어 있습니다.");
		}

		Accommodation accommodation = accommodationRepository.save(Accommodation.create(
				hostBusinessId,
				request.regionId(),
				request.categoryId(),
				request.name().trim(),
				normalizeDescription(request.description()),
				request.address().trim(),
				request.latitude(),
				request.longitude(),
				request.checkInTime(),
				request.checkOutTime(),
				LocalDateTime.now(clock)));
		Long accommodationId = accommodation.getId();

		// 사진: 보낸 순서대로 sort_order 0, 1, 2 … (0번이 대표 사진)
		List<String> imageUrls = request.imageUrls() == null ? List.of() : request.imageUrls();
		accommodationImageRepository.saveAll(IntStream.range(0, imageUrls.size())
				.mapToObj(index -> AccommodationImage.of(accommodationId, imageUrls.get(index).trim(), index))
				.toList());

		accommodationAmenityRepository.saveAll(amenityIds.stream()
				.map(amenityId -> AccommodationAmenity.of(accommodationId, amenityId))
				.toList());

		return new HostAccommodationCreateResponse(
				accommodationId,
				accommodation.getName(),
				accommodation.getListingStatus().name());
	}

	/** 앞뒤 공백을 지우고, 비어 있으면 null(소개 없음)로 저장합니다. 줄바꿈은 그대로 둡니다. */
	private static String normalizeDescription(String description) {
		if (description == null || description.isBlank()) {
			return null;
		}
		return description.strip();
	}
}
