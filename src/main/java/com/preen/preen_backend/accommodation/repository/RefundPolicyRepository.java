package com.preen.preen_backend.accommodation.repository;

import com.preen.preen_backend.accommodation.domain.RefundPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RefundPolicyRepository extends JpaRepository<RefundPolicy, Long> {

    List<RefundPolicy> findByAccommodationIdAndActiveTrue(Long accommodationId);

    List<RefundPolicy> findByAccommodationIdInAndActiveTrue(Collection<Long> accommodationIds);

    Optional<RefundPolicy> findFirstByAccommodationIdAndActiveTrueOrderByIdDesc(Long accommodationId);

    long countByAccommodationId(Long accommodationId);
}
