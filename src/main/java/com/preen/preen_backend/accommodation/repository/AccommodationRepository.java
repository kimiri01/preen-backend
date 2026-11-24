package com.preen.preen_backend.accommodation.repository;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.preen.preen_backend.accommodation.domain.Accommodation;
import com.preen.preen_backend.accommodation.domain.Accommodation.ListingStatus;

public interface AccommodationRepository extends JpaRepository<Accommodation, Long> {

	/** 검색 조건(숙소명·카테고리·지역·인원·날짜)으로 공개 숙소를 조회합니다. */
	@Query("""
			select a from Accommodation a
			where a.listingStatus = :listingStatus
			  and (:namePattern is null or lower(a.name) like lower(:namePattern) escape '!')
			  and (:categoryId is null or a.categoryId = :categoryId)
			  and (:regionId is null or a.regionId = :regionId)
			  and ((:guestCount is null and :dateFilter = false) or exists (
			        select r.id from Room r
			        where r.accommodationId = a.id
			          and r.status = com.preen.preen_backend.accommodation.domain.Room.RoomStatus.ACTIVE
			          and (:guestCount is null or r.maxCapacity >= :guestCount)
			          and (:dateFilter = false or (select count(i) from RoomInventory i
			               where i.roomId = r.id
			                 and i.stayDate >= :checkIn
			                 and i.stayDate < :checkOut
			                 and i.closed = false
			                 and (i.totalCount - i.bookedCount - i.heldCount) > 0) = :nights)))
			""")
	Page<Accommodation> search(
			@Param("listingStatus") ListingStatus listingStatus,
			@Param("namePattern") String namePattern,
			@Param("categoryId") Long categoryId,
			@Param("regionId") Long regionId,
			@Param("guestCount") Integer guestCount,
			@Param("dateFilter") boolean dateFilter,
			@Param("checkIn") LocalDate checkIn,
			@Param("checkOut") LocalDate checkOut,
			@Param("nights") long nights,
			Pageable pageable);

}
