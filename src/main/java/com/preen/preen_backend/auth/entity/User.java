package com.preen.preen_backend.auth.entity;

import com.preen.preen_backend.common.entity.BaseTimeEntity;
import com.preen.preen_backend.common.security.UserRole;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

// 회원의 식별자, 권한과 계정 상태를 저장합니다.
@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    // 일반 회원 권한과 활성 상태로 신규 회원을 생성합니다.
    public static User guest() {
        User user = new User();
        user.role = UserRole.GUEST;
        user.status = "ACTIVE";
        return user;
    }
}
