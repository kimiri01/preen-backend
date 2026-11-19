package com.preen.preen_backend.auth.repository;

import com.preen.preen_backend.auth.entity.UserCredentials;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// 회원의 비밀번호 인증 정보를 저장하고 조회합니다.
public interface UserCredentialsRepository extends JpaRepository<UserCredentials, Long> {
    // 같은 회원의 여러 로그인 요청이 실패 기록을 동시에 바꾸지 않도록 행을 잠급니다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select credentials from UserCredentials credentials where credentials.userId = :userId")
    Optional<UserCredentials> findForUpdate(@Param("userId") Long userId);
}
