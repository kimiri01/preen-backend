package com.preen.preen_backend.inventory.service;


import com.preen.preen_backend.accommodation.dto.RoomInfo;
import com.preen.preen_backend.accommodation.service.AccommodationQueryService;
import com.preen.preen_backend.common.BaseException;
import com.preen.preen_backend.common.exception.BusinessException;
import com.preen.preen_backend.inventory.domain.RoomInventory;
import com.preen.preen_backend.inventory.dto.InventoryDtos.*;
import com.preen.preen_backend.inventory.exception.InventoryErrorCode;
import com.preen.preen_backend.inventory.repository.InventoryHoldRepository;
import com.preen.preen_backend.inventory.repository.RoomInventoryRepository;
import com.preen.preen_backend.inventory.repository.RoomInventoryRepository.NightView;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.preen.preen_backend.inventory.dto.InventoryDtos.NightPrice;
import com.preen.preen_backend.inventory.domain.InventoryHold;
import com.preen.preen_backend.inventory.domain.InventoryHold.HoldStatus;
import com.preen.preen_backend.inventory.repository.InventoryHoldRepository.HoldView;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class InventoryService {


    /** 한 번에 조회·예약할 수 있는 최대 박수. */
    public static final int MAX_NIGHTS = 30;
    /** 호스트가 한 번에 설정·조회할 수 있는 최대 일수. */
    private static final int MAX_SET_DAYS = 366;

    private final InventoryHoldRepository holdRepository;
    private final RoomInventoryRepository inventoryRepository;
    private final AccommodationQueryService accommodationQueryService;
    private final InventoryProperties properties;

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


    // ---------- 호스트 ----------

    /*
     * 기간별 재고 조회 (startDate부터 endDate까지, 양 끝 포함). 본인 소유의 객실인지 먼저 검사한다.
     * 지난 날짜도 조회할 수 있다. 재고 행이 없는 날짜는 결과에 나오지 않는다(판매하지 않는 날).
     */
    @Transactional(readOnly = true)
    public List<InventoryResponse> getInventories(Long userId, Long roomId, LocalDate start, LocalDate end){
        accommodationQueryService.assertRoomOwner(roomId, userId);


        if(end.isBefore(start)){
            throw new BusinessException(InventoryErrorCode.INVALID_DATE_RANGE);
        }
        if(ChronoUnit.DAYS.between(start, end) >= MAX_SET_DAYS){
            throw new BusinessException(InventoryErrorCode.DATE_RANGE_TOO_LONG);
        }
        return inventoryRepository.findByRoomIdAndStayDateBetweenOrderByStayDateAsc(roomId, start, end)
                .stream()
                .map(InventoryResponse::from)
                .toList();
    }


    /** 기간별 수량, 가격, 판매 마감 설정. 본인 소유의 객실인지 먼저 검사한다. */
    @Transactional
    public List<InventoryResponse> setInventories(Long userId, Long roomId, SetInventoriesRequest request) {
        accommodationQueryService.assertRoomOwner(roomId, userId);
        validateSetRequest(request);

        // 없는 날짜는 먼저 만들고(동시에 만들어져도 무시), 그 다음 전체를 읽어 요청 값으로 바꾼다
        createMissingDates(roomId, request);
        List<RoomInventory> rows = inventoryRepository.findByRoomIdAndStayDateBetweenOrderByStayDateAsc(
                roomId, request.startDate(), request.endDate());
        rows.forEach(row -> applyChange(row, request));
        return rows.stream().map(InventoryResponse::from).toList();
    }



    /** 기간이 오늘 이후이고 최대 일수 이내인지, 바꿀 값이 하나라도 있는지 검사한다. */
    private void validateSetRequest(SetInventoriesRequest request) {
        LocalDate start = request.startDate();
        LocalDate end = request.endDate();
        if (end.isBefore(start) || start.isBefore(LocalDate.now())) {
            throw new BusinessException(InventoryErrorCode.INVALID_DATE_RANGE);
        }
        if (ChronoUnit.DAYS.between(start, end) >= MAX_SET_DAYS) {
            throw new BusinessException(InventoryErrorCode.DATE_RANGE_TOO_LONG);
        }
        if (request.totalCount() == null && request.price() == null && request.closed() == null) {
            throw new BusinessException(InventoryErrorCode.NOTHING_TO_UPDATE);
        }
    }


    /** 기간 중 재고 행이 없는 날짜를 만든다. 요청에 없는 수량·가격은 기본값(마지막 수량, 객실 기본가)을 쓴다. */
    private void createMissingDates(Long roomId, SetInventoriesRequest request) {
        LocalDate start = request.startDate();
        LocalDate end = request.endDate();
        Set<LocalDate> existing = new HashSet<>(inventoryRepository.findDates(roomId, start, end));
        RoomInfo room = accommodationQueryService.getRoomInfo(roomId);
        int newRowCount = request.totalCount() != null ? request.totalCount() : baseTotalCount(roomId);
        BigDecimal newRowPrice = request.price() != null ? request.price() : room.nightlyPrice();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            if (!existing.contains(date)) {
                inventoryRepository.insertIfAbsent(roomId, date, newRowCount, newRowPrice);
            }
        }
    }

    /** 수량을 줄일 때는 이미 예약·선점된 수량보다 적게 할 수 없다. */
    private void applyChange(RoomInventory row, SetInventoriesRequest request) {
        if (request.totalCount() != null && request.totalCount() < row.getBookedCount() + row.getHeldCount()) {
            throw new BusinessException(InventoryErrorCode.TOTAL_COUNT_TOO_SMALL);
        }
        row.change(request.totalCount(), request.price(), request.closed());
    }





    // ---------- 스케줄러 ----------

    /**
     * 판매 중인 모든 객실에 대해 오늘부터 N일 뒤까지 없는 날짜의 재고 행을 만든다.
     * 가격은 객실 기본가(nightly_price)를 쓴다. 이미 있는 행은 건드리지 않는다.
     *
     * @return 새로 만든 행 수
     */
    @Transactional
    public int generateUpcoming() {
        LocalDate today = LocalDate.now();
        LocalDate last = today.plusDays(properties.generateDays());
        int created = 0;
        for (RoomInfo room : accommodationQueryService.getAllActiveRooms()) {
            Set<LocalDate> existing = new HashSet<>(inventoryRepository.findDates(room.roomId(), today, last));
            if (existing.size() > properties.generateDays()) {
                continue;
            }
            int totalCount = baseTotalCount(room.roomId());
            for (LocalDate date = today; !date.isAfter(last); date = date.plusDays(1)) {
                if (!existing.contains(date)) {
                    created += inventoryRepository.insertIfAbsent(room.roomId(), date, totalCount, room.nightlyPrice());
                }
            }
        }
        return created;
    }






    // ---------- 내부 ----------



    /** 그 객실의 가장 마지막 날짜 재고의 수량을 이어받는다. 재고가 하나도 없으면 설정값을 쓴다. */
    private int baseTotalCount(Long roomId) {
        return inventoryRepository.findFirstByRoomIdOrderByStayDateDesc(roomId)
                .map(RoomInventory::getTotalCount)
                .orElse(properties.defaultTotalCount());
    }
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
