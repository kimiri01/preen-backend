package com.preen.preen_backend.accommodation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.preen.preen_backend.accommodation.domain.Amenity;

/** [kny] 숙소 상세페이지 "시설/서비스" 조회 저장소 */
public interface AmenityRepository extends JpaRepository<Amenity, Long> {

	/** 숙소 하나에 연결된 편의시설을 등록 순서(id)대로 가져옵니다. (accommodation_amenities 연결 테이블) */
	@Query("""
			select m from Amenity m
			where m.id in (
			    select l.id.amenityId from AccommodationAmenity l
			    where l.id.accommodationId = :accommodationId)
			order by m.id
			""")
	List<Amenity> findAllByAccommodationId(@Param("accommodationId") Long accommodationId);
}
