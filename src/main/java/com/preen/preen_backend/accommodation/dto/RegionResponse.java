package com.preen.preen_backend.accommodation.dto;

import com.preen.preen_backend.accommodation.domain.Region;

/** code는 메인 화면 지역 버튼과 약속한 고정 코드입니다. 코드가 없는 지역은 null입니다. (V101) */
public record RegionResponse(Long id, String code, String name, int level) {

	public static RegionResponse from(Region region) {
		return new RegionResponse(region.getId(), region.getCode(), region.getName(), region.getLevel());
	}

}
