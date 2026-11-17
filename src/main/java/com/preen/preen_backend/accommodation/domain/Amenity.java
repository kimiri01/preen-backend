package com.preen.preen_backend.accommodation.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 편의시설 마스터. 초기 데이터는 마이그레이션으로 넣는다. */
@Entity
@Table(name = "amenities")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Amenity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;
}
