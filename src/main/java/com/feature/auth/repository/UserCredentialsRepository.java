package com.feature.auth.repository;

import com.feature.auth.entity.UserCredentials;
import org.springframework.data.jpa.repository.JpaRepository;

// 회원의 비밀번호 인증 정보를 저장하고 조회합니다.
public interface UserCredentialsRepository extends JpaRepository<UserCredentials, Long> {}
