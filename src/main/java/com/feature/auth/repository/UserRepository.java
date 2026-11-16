package com.feature.auth.repository;

import com.feature.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

// 회원 기본 정보를 저장하고 조회합니다.
public interface UserRepository extends JpaRepository<User, Long> {}
