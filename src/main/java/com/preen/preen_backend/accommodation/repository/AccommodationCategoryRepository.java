package com.preen.preen_backend.accommodation.repository;

import com.preen.preen_backend.accommodation.domain.AccommodationCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccommodationCategoryRepository extends JpaRepository<AccommodationCategory, Long> {
}
