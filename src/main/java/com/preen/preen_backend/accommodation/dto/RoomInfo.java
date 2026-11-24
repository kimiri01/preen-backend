package com.preen.preen_backend.accommodation.dto;

import java.math.BigDecimal;

// ---------- 다른 feature용 ----------

/**
 * 예약·재고가 쓰는 객실 정보.
 *
 * @param sellable 숙소가 LISTED이고 객실이 ACTIVE인지
 */
public record RoomInfo(Long roomId, Long accommodationId, String accommodationName, String roomName,
                       int capacity, int maxCapacity, BigDecimal extraPersonFee, BigDecimal nightlyPrice,
                       boolean sellable) {
}
