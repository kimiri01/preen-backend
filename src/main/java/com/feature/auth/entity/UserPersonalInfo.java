package com.feature.auth.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.*;

// 회원의 이름, 암호화 이메일과 생년월일을 저장합니다.
@Getter
@Entity
@Table(name = "user_personal_info")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPersonalInfo {
    @Id @Column(name = "user_id")
    private Long userId;

    @Column(length = 100)
    private String name;

    @Column(length = 1024)
    private String email;

    @Column(name = "email_hash", columnDefinition = "char(64)")
    private String emailHash;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    // 암호화 이메일과 조회용 해시를 회원 개인정보에 함께 기록합니다.
    public static UserPersonalInfo create(Long userId, String name, String encryptedEmail,
            String emailHash, LocalDate birthDate) {
        UserPersonalInfo info = new UserPersonalInfo();
        info.userId = userId;
        info.name = name;
        info.email = encryptedEmail;
        info.emailHash = emailHash;
        info.birthDate = birthDate;
        return info;
    }
}
