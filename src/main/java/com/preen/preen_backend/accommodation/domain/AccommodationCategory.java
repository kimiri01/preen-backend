package com.preen.preen_backend.accommodation.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 숙소 카테고리(호텔·리조트, 한옥, 독채·풀빌라 등) 마스터. 초기 데이터는 마이그레이션으로 넣는다. */
@Entity
@Table(name = "accommodation_categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccommodationCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** [kny] 프론트 메인 화면 버튼과 연결하는 고정 코드 (예: HOTEL). 없으면 null. V101에서 추가 */
    @Column(length = 30)
    private String code;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private int sortOrder;

    public static AccommodationCategory of(String name, int sortOrder) {
        AccommodationCategory category = new AccommodationCategory();
        category.name = name;
        category.sortOrder = sortOrder;
        return category;
    }
}
