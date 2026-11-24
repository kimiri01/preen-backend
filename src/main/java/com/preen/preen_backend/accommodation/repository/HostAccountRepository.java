package com.preen.preen_backend.accommodation.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.preen.preen_backend.accommodation.domain.Accommodation;

/**
 * [kny] 숙소 등록 전에 "이 회원이 호스트인지, 승인된 사업자가 있는지" 확인하는 조회 전용 저장소.
 * users·host_businesses 테이블은 아직 이 기능용 엔티티가 없어서, 엔티티를 새로 만들지 않고 조회 쿼리만 둡니다.
 */
public interface HostAccountRepository extends JpaRepository<Accommodation, Long> {

	/** 활성(ACTIVE) 상태이고 탈퇴하지 않은 HOST 회원이면 1, 아니면 0 */
	@Query(value = """
			SELECT COUNT(*) FROM users u
			WHERE u.id = :userId AND u.role = 'HOST' AND u.status = 'ACTIVE' AND u.deleted_at IS NULL
			""", nativeQuery = true)
	long countActiveHost(@Param("userId") Long userId);

	/** 회원의 승인(APPROVED)된 사업자 중 가장 최근 것의 ID. 없으면 빈 값 */
	@Query(value = """
			SELECT hb.id FROM host_businesses hb
			WHERE hb.host_user_id = :userId AND hb.approval_status = 'APPROVED'
			ORDER BY hb.id DESC
			LIMIT 1
			""", nativeQuery = true)
	Optional<Long> findApprovedHostBusinessId(@Param("userId") Long userId);
}
