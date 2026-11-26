package com.preen.preen_backend.accommodation.service;

import com.preen.preen_backend.accommodation.domain.*;
import com.preen.preen_backend.accommodation.dto.AccommodationDetailResponse;
import com.preen.preen_backend.accommodation.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * [kny] 숙소 상세페이지 조회.
 * 기존 테이블(숙소·카테고리·지역·사진·편의시설)만 읽고, 새 테이블은 만들지 않습니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccommodationDetailQueryServiceTest {

	private final AccommodationRepository accommodationRepository;
	private final CategoryRepository categoryRepository;
	private final RegionRepository regionRepository;
	private final AccommodationImageRepository accommodationImageRepository;
	private final AmenityRepository amenityRepository;

	/** 공개(LISTED) 숙소만 보여 줍니다. 없거나 숨김이면 404입니다. */
	public AccommodationDetailResponse getDetail(Long id) {
		Accommodation accommodation = accommodationRepository.findById(id)
				.filter(Accommodation::isListed)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "숙소를 찾을 수 없습니다."));

		String categoryName = categoryRepository.findById(accommodation.getCategoryId())
				.map(AccommodationCategory::getName)
				.orElse(null);
		String regionName = regionRepository.findById(accommodation.getRegionId())
				.map(Region::getName)
				.orElse(null);
		List<String> imageUrls = accommodationImageRepository
				.findAllByAccommodationIdInOrderBySortOrderAsc(List.of(id)).stream()
				.map(AccommodationImage::getImageUrl)
				.toList();
		List<String> amenities = amenityRepository.findAllByAccommodationId(id).stream()
				.map(Amenity::getName)
				.toList();

		return new AccommodationDetailResponse(
				accommodation.getId(),
				accommodation.getName(),
				accommodation.getDescription(),
				accommodation.getAddress(),
				accommodation.getRegionId(),
				regionName,
				accommodation.getCategoryId(),
				categoryName,
				imageUrls,
				amenities);
	}
}
