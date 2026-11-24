package com.preen.preen_backend.accommodation.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 숙소별 환불정책 (수정 대신 새 버전을 만들어 관리) */
@Entity
@Table(name = "refund_policies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefundPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long accommodationId;

    @Column(nullable = false, length = 20)
    private String version;

    @Column(columnDefinition = "text")
    private String policyText;

    @Column(nullable = false)
    private boolean active;

    public static RefundPolicy createActive(Long accommodationId, String version, String policyText) {
        RefundPolicy policy = new RefundPolicy();
        policy.accommodationId = accommodationId;
        policy.version = version;
        policy.policyText = policyText;
        policy.active = true;
        return policy;
    }

    public void deactivate() {
        this.active = false;
    }
}
