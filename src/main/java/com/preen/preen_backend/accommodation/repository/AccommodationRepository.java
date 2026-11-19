package com.preen.preen_backend.accommodation.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.preen.preen_backend.accommodation.domain.Accommodation;
import com.preen.preen_backend.accommodation.domain.Accommodation.ListingStatus;

public interface AccommodationRepository extends JpaRepository<Accommodation, Long> {



	List<Accommodation> findByHostBusinessIdOrderByIdDesc(Long hostBusinessId);





	/**
	 * 숙소 목록 조회. namePattern, categoryId, regionId, guestCount는 각각 null이면 조건을 적용하지 않습니다.
	 * namePattern은 숙소명에 대한 LIKE 패턴이고 이스케이프 문자는 '!'입니다. 대소문자는 구분하지 않습니다.
	 * guestCount가 있으면 최대 인원이 guestCount 이상인 ACTIVE 객실이 최소 한 개 있는 숙소만 남깁니다.
	 * dateFilter가 true이면 숙박 기간(checkIn ~ checkOut 전날)의 모든 날짜에
	 * 판매 가능한 재고가 있는 ACTIVE 객실이 최소 한 개 있는 숙소만 남깁니다.
	 * 인원과 날짜를 함께 쓰면 한 객실이 두 조건을 모두 만족해야 합니다.
	 * nights는 숙박 일수(checkOut - checkIn)입니다.
	 */
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
