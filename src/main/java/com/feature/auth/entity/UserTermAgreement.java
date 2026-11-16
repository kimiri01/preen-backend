package com.feature.auth.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

// 회원별 약관 동의 여부와 동의 시각을 저장합니다.
@Getter
@Entity
@Table(name = "user_term_agreements")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserTermAgreement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "term_id", nullable = false)
    private Long termId;

    @Column(name = "is_agreed", nullable = false)
    private boolean agreed;

    @Column(name = "agreed_at", nullable = false)
    private Instant agreedAt;

    // 회원이 선택한 약관의 동의 여부와 시각을 기록합니다.
    public static UserTermAgreement create(Long userId, Long termId, boolean agreed, Instant now) {
        UserTermAgreement agreement = new UserTermAgreement();
        agreement.userId = userId;
        agreement.termId = termId;
        agreement.agreed = agreed;
        agreement.agreedAt = now;
        return agreement;
    }
}
