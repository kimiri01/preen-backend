package com.preen.preen_backend.accommodation.repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.preen.preen_backend.accommodation.domain.Room;

/**
 * [kny] 숙소 카드의 "최저 1박 요금"을 구하는 조회 전용 저장소.
 * 팀원 파일(AccommodationRepository)을 고치지 않으려고 따로 만들었습니다.
 */
public interface RoomPriceRepository extends JpaRepository<Room, Long> {

	/** 숙소 하나의 최저 요금 (조회 결과 한 줄) */
	interface MinPrice {
		Long getAccommodationId();

		BigDecimal getMinNightlyPrice();
	}

	/** 숙소별로 판매 중(ACTIVE)인 객실의 기본가 중 가장 낮은 값을 한 번에 가져옵니다. (N+1 방지) */
	@Query("""
			select r.accommodationId as accommodationId, min(r.nightlyPrice) as minNightlyPrice
			from Room r
			where r.status = com.preen.preen_backend.accommodation.domain.Room.RoomStatus.ACTIVE
			  and r.accommodationId in :accommodationIds
			group by r.accommodationId
			""")
	List<MinPrice> findMinNightlyPrices(@Param("accommodationIds") Collection<Long> accommodationIds);

}
