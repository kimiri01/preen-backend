package com.preen.preen_backend.accommodation.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.preen.preen_backend.accommodation.domain.AccommodationAmenity;

/** [kny] 숙소-편의시설 연결(accommodation_amenities) 저장소. 숙소 등록 시 편의시설을 연결합니다. */
public interface AccommodationAmenityRepository
		extends JpaRepository<AccommodationAmenity, AccommodationAmenity.Key> {
}
