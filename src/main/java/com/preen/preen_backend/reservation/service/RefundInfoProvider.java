package com.preen.preen_backend.reservation.service;

import com.preen.preen_backend.reservation.dto.ReservationDtos.RefundInfo;
import java.util.Collection;
import java.util.Map;

/**
 * 예약 응답에 환불 정보를 싣기 위한 연결 지점.
 * 의존 방향이 payment → reservation이므로 인터페이스는 reservation에 두고 payment가 구현한다.
 */
public interface RefundInfoProvider {

    /** 예약 ID → 그 예약의 환불 정보. 환불 내역이 없는 예약은 결과에 없다. */
    Map<Long, RefundInfo> findByReservationIds(Collection<Long> reservationIds);
}
