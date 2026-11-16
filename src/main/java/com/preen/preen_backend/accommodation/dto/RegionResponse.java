package com.preen.preen_backend.accommodation.dto;

import com.preen.preen_backend.accommodation.domain.Region;

public record RegionResponse(Long id, String code, String name) {

	public static RegionResponse from(Region region) {
		return new RegionResponse(region.getId(), region.getCode(), region.getName());
	}

}
