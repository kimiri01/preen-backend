package com.preen.preen_backend.accommodation.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.preen.preen_backend.accommodation.domain.AccommodationImage;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccommodationImageRepository extends JpaRepository<AccommodationImage, Long> {

	/** 여러 숙소의 사진을 한 번에 가져옵니다. (sort_order 오름차순: 첫 번째가 대표 사진) */
	List<AccommodationImage> findAllByAccommodationIdInOrderBySortOrderAsc(Collection<Long> accommodationIds);

	List<AccommodationImage> findByAccommodationIdOrderBySortOrderAsc(Long accommodationId);


	@Modifying
	@Query("delete from AccommodationImage i where i.accommodationId = :accommodationId")
	void deleteAllByAccommodationId(@Param("accommodationId") Long accommodationId);


}
