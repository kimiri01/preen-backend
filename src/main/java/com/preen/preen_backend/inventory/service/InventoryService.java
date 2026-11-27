package com.preen.preen_backend.inventory.service;


import com.preen.preen_backend.common.exception.BusinessException;
import com.preen.preen_backend.inventory.exception.InventoryErrorCode;
import com.preen.preen_backend.inventory.repository.InventoryHoldRepository;
import com.preen.preen_backend.inventory.repository.RoomInventoryRepository;
import com.preen.preen_backend.inventory.repository.RoomInventoryRepository.NightView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.preen.preen_backend.inventory.dto.InventoryDtos.NightPrice;
import com.preen.preen_backend.inventory.domain.InventoryHold;
import com.preen.preen_backend.inventory.domain.InventoryHold.HoldStatus;
import com.preen.preen_backend.inventory.repository.InventoryHoldRepository.HoldView;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryService {


    /** 한 번에 조회·예약할 수 있는 최대 박수. */
    public static final int MAX_NIGHTS = 30;
    private final InventoryHoldRepository holdRepository;
    private final RoomInventoryRepository inventoryRepository;

    // ---------- 다른 feature용: 선점·확정·해제 (모두 예약 ID 기준) ----------

    /**
     * 숙박일(checkIn 이상 checkOut 미만)마다 재고를 1개씩 선점한다.
     * 날짜 오름차순으로 조건부 UPDATE를 하고, 하루라도 실패하면 예외를 던져 호출한 트랜잭션 전체를 롤백시킨다.
     *
     * @return 날짜별 판매가 (날짜 오름차순)
     */
    @Transactional
    public List<NightPrice> hold(Long reservationId, Long roomId, LocalDate checkIn, LocalDate checkOut,
                                 LocalDateTime expiresAt) {
        long nights = validateStay(checkIn, checkOut);
        List<NightView> rows = inventoryRepository.findNights(roomId, checkIn, checkOut);
        if (rows.size() != nights) {
            // 재고 행이 없는 날짜가 있으면 판매하지 않는 날이다
            throw new BusinessException(InventoryErrorCode.SOLD_OUT);
        }
        for (NightView row : rows) {
            if (inventoryRepository.tryHold(row.getId()) == 0) {
                throw new BusinessException(InventoryErrorCode.SOLD_OUT);
            }
            holdRepository.save(InventoryHold.held(reservationId, row.getId(), expiresAt));
        }
        return rows.stream().map(r -> new NightPrice(r.getStayDate(), r.getPrice())).toList();
    }

    /**
     * 결제 완료: HELD인 선점을 CONFIRMED로 바꾸고 held_count - 1, booked_count + 1.
     * 이미 CONFIRMED거나 RELEASED인 행은 건드리지 않으므로 두 번 호출해도 결과가 같다.
     */
    @Transactional
    public void confirm(Long reservationId) {
        for (HoldView hold : holdRepository.findByReservation(reservationId)) {
            if (holdRepository.transition(hold.getId(), HoldStatus.HELD, HoldStatus.CONFIRMED) == 1) {
                inventoryRepository.moveHeldToBooked(hold.getRoomInventoryId());
            }
        }
    }

    /**
     * 만료·취소: hold 상태에 따라 해당 카운트를 되돌리고 RELEASED로 바꾼다.
     * HELD였으면 held_count - 1, CONFIRMED였으면 booked_count - 1. 두 번 호출해도 결과가 같다.
     */
    @Transactional
    public void release(Long reservationId) {
        for (HoldView hold : holdRepository.findByReservation(reservationId)) {
            if (holdRepository.transition(hold.getId(), HoldStatus.HELD, HoldStatus.RELEASED) == 1) {
                inventoryRepository.decreaseHeld(hold.getRoomInventoryId());
            } else if (holdRepository.transition(hold.getId(), HoldStatus.CONFIRMED, HoldStatus.RELEASED) == 1) {
                inventoryRepository.decreaseBooked(hold.getRoomInventoryId());
            }
        }
    }


    // ---------- 내부 ----------

    /** 1박 이상, 최대 박수 이하인지 검사하고 박수를 돌려준다. */
    static long validateStay(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) {
            throw new BusinessException(InventoryErrorCode.INVALID_DATE_RANGE);
        }
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights > MAX_NIGHTS) {
            throw new BusinessException(InventoryErrorCode.DATE_RANGE_TOO_LONG);
        }
        return nights;
    }

}
