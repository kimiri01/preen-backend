package com.preen.preen_backend.accommodation.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.preen.preen_backend.accommodation.domain.Accommodation;
import com.preen.preen_backend.accommodation.domain.ListingStatus;

public interface AccommodationRepository extends JpaRepository<Accommodation, Long> {

	/**
	 * 숙소 목록 조회.
	 * categoryId, regionId는 각각 null이면 "조건 없음"으로 처리합니다.
	 * (둘 다 null이면 전체 숙소, 둘 다 있으면 두 조건을 모두 만족하는 숙소)
	 *
	 * @EntityGraph: 숙소를 조회할 때 카테고리·지역도 한 번의 쿼리로 함께 가져옵니다.
	 * (없으면 숙소마다 카테고리·지역을 따로 조회하는 N+1 문제가 생깁니다.)
	 */
	@EntityGraph(attributePaths = { "category", "region" })
	@Query("""
			select a from Accommodation a
			where a.listingStatus = :listingStatus
			  and (:categoryId is null or a.category.id = :categoryId)
			  and (:regionId is null or a.region.id = :regionId)
			""")
	Page<Accommodation> search(
			@Param("listingStatus") ListingStatus listingStatus,
			@Param("categoryId") Long categoryId,
			@Param("regionId") Long regionId,
			Pageable pageable);

	/**
	 * 카테고리 하나의 공개 숙소를 pageable 개수만큼 조회합니다. ("전체 숙소" 카테고리 묶음에서 사용)
	 * Page 대신 List로 받아서, 전체 개수를 세는 쿼리(count)는 실행하지 않습니다.
	 * 메서드 이름의 Category_Id 는 "숙소의 category 안의 id" 라는 뜻입니다.
	 */
	@EntityGraph(attributePaths = { "category", "region" })
	List<Accommodation> findByListingStatusAndCategory_Id(ListingStatus listingStatus, Long categoryId,
			Pageable pageable);

	/**
	 * ID 목록에 해당하는 숙소를 한 번에 조회합니다. ("최근 본 숙소"에서 사용)
	 * 공개(LISTED) 상태인 숙소만 돌려주며, 결과 순서는 보장되지 않습니다.
	 */
	@EntityGraph(attributePaths = { "category", "region" })
	List<Accommodation> findByIdInAndListingStatus(Collection<Long> ids, ListingStatus listingStatus);

	/**
	 * 이미지 경로가 prefix로 시작하는 숙소를 지웁니다. (예전 예시 숙소 13건 정리용)
	 * 이름 규칙(deleteBy...)만으로 JPA가 삭제 쿼리를 만들어 줍니다. 지운 건수를 돌려줍니다.
	 */
	long deleteByThumbnailUrlStartingWith(String prefix);

	/** 이미지 경로가 suffix로 끝나는 숙소를 지웁니다. (일러스트 .svg 예시 숙소 정리용) */
	long deleteByThumbnailUrlEndingWith(String suffix);

}
