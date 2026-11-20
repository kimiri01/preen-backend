package com.preen.preen_backend.host.repository;

import com.preen.preen_backend.host.domain.HostBusiness;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HostBusinessRepository extends JpaRepository<HostBusiness, Long> {
    Optional<HostBusiness> findFirstByHostUserIdOrderByIdAsc(Long hostUserId);


    boolean existsByHostUserId(Long hostUserId);

    boolean existsByBusinessRegistrationNo(String businessRegistrationNo);



}
