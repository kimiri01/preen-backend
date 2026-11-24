package com.preen.preen_backend.accommodation.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.preen.preen_backend.accommodation.domain.Accommodation;
import com.preen.preen_backend.accommodation.domain.Accommodation.ListingStatus;

/** 메인 페이지 숙소 카드 조회 저장소 */
public interface AccommodationCardRepository extends JpaRepository<Accommodation, Long> {

	/** 카테고리 하나의 공개 숙소를 N개만 조회합니다. */
	List<Accommodation> findByListingStatusAndCategoryId(ListingStatus listingStatus, Long categoryId,
			Pageable pageable);

	/** ID 목록 중 공개 상태인 숙소만 가져옵니다. (순서는 보장하지 않음) */
	List<Accommodation> findByIdInAndListingStatus(Collection<Long> ids, ListingStatus listingStatus);

}
