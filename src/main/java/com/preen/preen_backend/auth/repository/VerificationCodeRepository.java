package com.preen.preen_backend.auth.repository;

import com.preen.preen_backend.auth.entity.VerificationCode;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

// 이메일 인증 데이터를 생성하고 잠금을 걸어 조회합니다.
public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {
    // 최초 인증 요청이면 잠금에 사용할 행을 만들고, 기존 행은 유지합니다.
    @Modifying
    @Query(value = """
        INSERT IGNORE INTO verification_codes
        (target_type, target_hash, purpose, code_hash, attempt_count, expires_at, created_at)
        VALUES ('EMAIL', :targetHash, 'SIGNUP', '', 0, :now, :now)
        """, nativeQuery = true)
    void ensureTarget(@Param("targetHash") String targetHash, @Param("now") Instant now);

    // 같은 이메일의 인증·가입 처리가 겹치지 않도록 조회 행을 잠급니다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select code from VerificationCode code where code.targetHash = :targetHash "
            + "and code.targetType = 'EMAIL' and code.purpose = 'SIGNUP'")
    java.util.Optional<VerificationCode> findForUpdate(@Param("targetHash") String targetHash);
}
