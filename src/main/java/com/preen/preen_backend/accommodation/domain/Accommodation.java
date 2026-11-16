package com.preen.preen_backend.accommodation.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 숙소. accommodation 테이블과 연결됩니다.
 * 카테고리별·지역별 목록 조회에 필요한 최소 컬럼만 두었습니다.
 */
@Entity
@Table(name = "accommodation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Accommodation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** 숙소는 카테고리 하나에만 속합니다. (기능명세서 STAY-002 비고) */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "category_id", nullable = false)
	private Category category;

	/** 숙소가 위치한 지역. "지역으로 찾기"의 조회 조건으로 씁니다. */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "region_id", nullable = false)
	private Region region;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(nullable = false, length = 255)
	private String address;

	@Column(name = "thumbnail_url", length = 500)
	private String thumbnailUrl;

	/** 최저 1박 요금(원) */
	@Column(name = "min_nightly_price", nullable = false)
	private int minNightlyPrice;

	@Enumerated(EnumType.STRING)
	@Column(name = "listing_status", nullable = false, length = 20)
	private ListingStatus listingStatus;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	public Accommodation(Category category, Region region, String name, String address, String thumbnailUrl,
			int minNightlyPrice, ListingStatus listingStatus) {
		this.category = category;
		this.region = region;
		this.name = name;
		this.address = address;
		this.thumbnailUrl = thumbnailUrl;
		this.minNightlyPrice = minNightlyPrice;
		this.listingStatus = listingStatus;
	}

	/** DB에 처음 저장되기 직전에 등록 시각을 채웁니다. */
	@PrePersist
	void onCreate() {
		this.createdAt = LocalDateTime.now();
	}

}
