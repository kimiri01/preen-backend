package com.preen.preen_backend.accommodation.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.preen.preen_backend.accommodation.domain.AccommodationImage;

public interface AccommodationImageRepository extends JpaRepository<AccommodationImage, Long> {

	/** 여러 숙소의 사진을 한 번에 가져옵니다. (sort_order 오름차순: 첫 번째가 대표 사진) */
	List<AccommodationImage> findAllByAccommodationIdInOrderBySortOrderAsc(Collection<Long> accommodationIds);

}
