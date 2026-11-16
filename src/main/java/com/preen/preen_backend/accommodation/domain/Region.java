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

/** 지역 (예: 서울, 제주도). "지역으로 찾기"에서 사용합니다. region 테이블과 연결됩니다. */
@Entity
@Table(name = "region")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Region {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** 프론트엔드와 약속한 고정 코드 (예: SEOUL) */
	@Column(nullable = false, unique = true, length = 30)
	private String code;

	/** 화면에 보여 줄 이름 (예: 서울) */
	@Column(nullable = false, unique = true, length = 50)
	private String name;

	/** 화면에 보여 줄 순서 (작은 숫자가 먼저) */
	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	public Region(String code, String name, int sortOrder) {
		this.code = code;
		this.name = name;
		this.sortOrder = sortOrder;
	}

}
