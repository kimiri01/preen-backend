package com.preen.preen_backend.accommodation.repository;

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

}
