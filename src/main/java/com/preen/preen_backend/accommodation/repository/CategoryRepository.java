package com.preen.preen_backend.accommodation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.preen.preen_backend.accommodation.domain.AccommodationCategory;

public interface CategoryRepository extends JpaRepository<AccommodationCategory, Long> {

	List<AccommodationCategory> findAllByOrderBySortOrderAsc();

}
