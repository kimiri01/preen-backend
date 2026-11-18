package com.preen.preen_backend.accommodation.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 지역 트리 (시·도 > 시·군·구). 초기 데이터는 마이그레이션으로 넣는다. */
@Entity
@Table(name = "regions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Region {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** [kny] 프론트 메인 화면 지역 버튼과 연결하는 고정 코드 (예: SEOUL). 없으면 null. V101에서 추가 */
    @Column(length = 30)
    private String code;

    private Long parentId;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, columnDefinition = "tinyint")
    private int level;

    /** level 1: 시·도, level 2: 시·군·구 */
    public static Region of(Long parentId, String name, int level) {
        Region region = new Region();
        region.parentId = parentId;
        region.name = name;
        region.level = level;
        return region;
    }
}
