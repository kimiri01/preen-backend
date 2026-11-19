package com.preen.preen_backend.auth.repository;

import com.preen.preen_backend.auth.entity.UserPersonalInfo;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

// 회원 개인정보를 저장하고 이메일 중복 여부를 조회합니다.
public interface UserPersonalInfoRepository extends JpaRepository<UserPersonalInfo, Long> {
    // 이메일 원문 대신 해시로 가입 여부를 확인합니다.
    boolean existsByEmailHash(String emailHash);

    // 로그인할 회원을 이메일 해시로 찾습니다.
    Optional<UserPersonalInfo> findByEmailHash(String emailHash);
}
