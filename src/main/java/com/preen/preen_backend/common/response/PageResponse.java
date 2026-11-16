package com.preen.preen_backend.common.response;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/**
 * 목록(페이지) 응답 형식입니다. (기능명세서 COMMON-004)
 * page는 0부터 시작합니다.
 */
public record PageResponse<T>(
		List<T> content,
		int page,
		int size,
		long totalElements,
		int totalPages,
		boolean hasNext) {

	/** DB에서 조회한 Page(엔티티)를 응답용 DTO 목록으로 바꿔 담습니다. */
	public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
		return new PageResponse<>(
				page.getContent().stream().map(mapper).toList(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.hasNext());
	}

}
