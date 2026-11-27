package com.preen.preen_backend.reservation.repository;

import com.preen.preen_backend.reservation.domain.ReservationStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationStatusHistoryRepository extends JpaRepository<ReservationStatusHistory, Long> {
}
