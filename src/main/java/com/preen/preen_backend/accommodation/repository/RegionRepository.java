package com.preen.preen_backend.accommodation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.preen.preen_backend.accommodation.domain.Region;

public interface RegionRepository extends JpaRepository<Region, Long> {

	/** 지정한 단계(level)의 지역을 id 순서(등록 순서)로 가져옵니다. */
	List<Region> findAllByLevelOrderByIdAsc(int level);

}
