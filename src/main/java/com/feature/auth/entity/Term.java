package com.feature.auth.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

// 약관의 종류, 버전, 본문과 필수 여부를 저장합니다.
@Getter
@Entity
@Table(name = "terms")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Term {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String type;

    @Column(nullable = false, length = 50)
    private String version;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(nullable = false)
    private boolean required;

    @Column(name = "effective_at", nullable = false)
    private Instant effectiveAt;
}
