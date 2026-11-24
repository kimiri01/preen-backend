package com.preen.preen_backend.reservation.service;

import com.preen.preen_backend.reservation.dto.ReservationDtos.RefundInfo;
import java.util.Collection;
import java.util.Map;
import org.springframework.stereotype.Service;


@Service
public class FakeRefundInfoProvider implements RefundInfoProvider {

    @Override
    public Map<Long, RefundInfo> findByReservationIds(Collection<Long> reservationIds) {
        // 환불 내역이 아예 없는 것처럼 빈 Map을 반환합니다
        return Map.of();
    }
}
