package com.feature.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import lombok.Getter;
import org.springframework.data.annotation.LastModifiedDate;

// 엔티티가 공통으로 사용하는 생성·수정 시각을 관리합니다.
@Getter
@MappedSuperclass
public abstract class BaseTimeEntity extends BaseCreatedEntity {
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
