package com.preen.preen_backend.accommodation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 숙박 유형 카테고리 (예: 호텔, 풀빌라). category 테이블과 연결됩니다. */
@Entity
@Table(name = "category")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * 프론트엔드와 약속한 고정 코드 (예: HOTEL).
	 * 화면에 보이는 이름(name)이 바뀌어도 코드는 그대로라 연결이 끊기지 않습니다.
	 */
	@Column(nullable = false, unique = true, length = 30)
	private String code;

	/** 화면에 보여 줄 이름 (예: 호텔) */
	@Column(nullable = false, unique = true, length = 50)
	private String name;

	/** 화면에 보여 줄 순서 (작은 숫자가 먼저) */
	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	public Category(String code, String name, int sortOrder) {
		this.code = code;
		this.name = name;
		this.sortOrder = sortOrder;
	}

}
