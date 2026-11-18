package com.preen.preen_backend.auth.repository;

import com.preen.preen_backend.auth.entity.UserTermAgreement;
import org.springframework.data.jpa.repository.JpaRepository;

// 회원별 약관 동의 내역을 저장하고 조회합니다.
public interface UserTermAgreementRepository extends JpaRepository<UserTermAgreement, Long> {}
