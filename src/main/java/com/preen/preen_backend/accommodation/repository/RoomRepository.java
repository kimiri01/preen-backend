package com.preen.preen_backend.accommodation.repository;

import com.preen.preen_backend.accommodation.domain.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findByAccommodationIdOrderByIdAsc(Long accommodationId);

    List<Room> findByAccommodationIdIn(Collection<Long> accommodationIds);

    List<Room> findByStatus(Room.RoomStatus status);

    boolean existsByAccommodationIdAndStatus(Long accommodationId, Room.RoomStatus status);

    long countByAccommodationIdAndStatus(Long accommodationId, Room.RoomStatus status);
}
