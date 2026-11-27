package com.preen.preen_backend.reservation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 결제 제한 시간이 지난 PENDING 예약을 주기적으로 취소하고 선점 재고를 되돌린다.
 * 결제 연동 전에도 동작한다. 서버가 여러 대여도 예약 행을 잠그고 상태를 다시 확인하므로 중복 처리되지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationExpiryScheduler {

    private final ReservationService reservationService;

    @Scheduled(fixedDelayString = "${staybook.reservation.expire-interval-ms:60000}",
            initialDelayString = "${staybook.reservation.expire-interval-ms:60000}")
    public void expireOverdueReservations() {
        int expired = reservationService.expireOverdue();
        if (expired > 0) {
            log.info("결제 시간이 지난 예약 {}건을 취소하고 재고를 해제했습니다.", expired);
        }
    }
}
