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
