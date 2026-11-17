package com.preen.preen_backend.accommodation.dto;

import com.preen.preen_backend.accommodation.domain.Region;

/** code는 domain2 지역에 없어서 현재 null입니다. */
public record RegionResponse(Long id, String code, String name, int level) {

	public static RegionResponse from(Region region) {
		return new RegionResponse(region.getId(), null, region.getName(), region.getLevel());
	}

}
