package com.preen.preen_backend.accommodation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.preen.preen_backend.accommodation.domain.Region;

public interface RegionRepository extends JpaRepository<Region, Long> {

	List<Region> findAllByOrderBySortOrderAsc();

}
