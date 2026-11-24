package com.preen.preen_backend.accommodation.repository;

import com.preen.preen_backend.accommodation.domain.RoomImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface RoomImageRepository extends JpaRepository<RoomImage, Long> {


    List<RoomImage> findByRoomIdInOrderBySortOrderAsc(Collection<Long> roomIds);


    @Modifying
    @Query("delete from RoomImage i where i.roomId = :roomId")
    void deleteAllByRoomId(@Param("roomId") Long roomId);
}
