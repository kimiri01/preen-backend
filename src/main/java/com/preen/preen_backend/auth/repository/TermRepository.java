package com.preen.preen_backend.auth.repository;

import com.preen.preen_backend.auth.entity.Term;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

// 가입에 사용할 약관 버전과 시행 여부를 조회합니다.
public interface TermRepository extends JpaRepository<Term, Long> {
    // 요청한 버전 중 현재 시각에 시행 중인 약관을 조회합니다.
    List<Term> findByVersionAndEffectiveAtLessThanEqual(String version, Instant now);
}
