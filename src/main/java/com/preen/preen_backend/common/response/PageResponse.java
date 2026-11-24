package com.preen.preen_backend.common.response;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/** 목록(페이지) 응답 형식 (page는 0부터 시작) */
public record PageResponse<T>(
		List<T> content,
		int page,
		int size,
		long totalElements,
		int totalPages,
		boolean hasNext) {

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
