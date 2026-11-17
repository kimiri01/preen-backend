package com.preen.preen_backend.accommodation.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.preen.preen_backend.accommodation.domain.Accommodation;
import com.preen.preen_backend.accommodation.domain.Accommodation.ListingStatus;

/**
 * [kny] 메인 페이지 카드 뷰("전체 숙소" 묶음)와 "최근 본 숙소"용 조회 저장소.
 * 팀원 파일(AccommodationRepository)을 고치지 않으려고 따로 만들었습니다.
 */
public interface AccommodationCardRepository extends JpaRepository<Accommodation, Long> {

	/**
	 * 카테고리 하나의 공개 숙소를 pageable 순서대로 N개만 가져옵니다.
	 * 반환형이 List라서 "전체 개수"를 세는 쿼리는 실행되지 않습니다.
	 */
	List<Accommodation> findByListingStatusAndCategoryId(ListingStatus listingStatus, Long categoryId,
			Pageable pageable);

	/** ID 목록 중 공개 상태인 숙소만 가져옵니다. (순서는 보장하지 않음) */
	List<Accommodation> findByIdInAndListingStatus(Collection<Long> ids, ListingStatus listingStatus);

}
