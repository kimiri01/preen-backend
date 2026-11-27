package com.preen.preen_backend.reservation.repository;

import com.preen.preen_backend.reservation.domain.ReservationPriceItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ReservationPriceItemRepository extends JpaRepository<ReservationPriceItem, Long> {

    List<ReservationPriceItem> findByReservationIdInOrderByIdAsc(Collection<Long> reservationIds);
}
