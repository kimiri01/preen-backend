package com.feature.auth.repository;

import com.feature.auth.entity.UserPersonalInfo;
import org.springframework.data.jpa.repository.JpaRepository;

// 회원 개인정보를 저장하고 이메일 중복 여부를 조회합니다.
public interface UserPersonalInfoRepository extends JpaRepository<UserPersonalInfo, Long> {
    // 이메일 원문 대신 해시로 가입 여부를 확인합니다.
    boolean existsByEmailHash(String emailHash);
}
