package com.preen.preen_backend.reservation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.preen.preen_backend.accommodation.dto.RoomInfo;
import com.preen.preen_backend.reservation.domain.Reservation;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import tools.jackson.databind.JsonNode;


public class ReservationDtos {


    /**
     * guestName, guestPhone을 보내지 않으면 회원 정보의 이름과 전화번호를 쓴다.
     */
    public record CreateRequest(
            @NotNull(message = "객실을 선택해 주세요.") Long roomId,
            @NotNull(message = "체크인 날짜를 입력해 주세요.") LocalDate checkIn,
            @NotNull(message = "체크아웃 날짜를 입력해 주세요.") LocalDate checkOut,
            @NotNull @Min(value = 1, message = "인원은 1명 이상이어야 합니다.")
            @Max(value = 100, message = "인원이 너무 많습니다.") Integer guestCount,
            @Size(max = 50) @Pattern(regexp = ".*\\S.*", message = "예약자 이름을 입력해 주세요.") String guestName,
            @Pattern(regexp = "^[0-9\\-+ ]{9,20}$", message = "전화번호 형식이 올바르지 않습니다.") String guestPhone
    ) {
    }


    public record PriceItemResponse(String itemType, String name, BigDecimal amount) {
    }

    /**
     * 호스트 예약 목록 검색 조건. 모두 선택값이며, 보내지 않은 조건은 적용하지 않는다. from·to는 체크인 날짜 기준.
     */
    public record HostReservationSearch(
            Long accommodationId,
            Reservation.Status status,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
    }

    /**
     * 환불 내역.
     *
     * @param refundAmount  환불액
     * @param penaltyAmount 위약금 (결제 금액 - 환불액)
     * @param status        REQUESTED(처리 중), COMPLETED(환불 완료), FAILED(환불 실패, 수동 처리 대상)
     * @param refundedAt    환불 완료 시각. 완료 전이면 null
     */
    public record RefundInfo(BigDecimal refundAmount, BigDecimal penaltyAmount, String status,
                             LocalDateTime refundedAt) {
    }


    /**
     * @param holdExpiresAt PENDING 상태에서 결제를 마쳐야 하는 시각
     * @param refundPolicy  예약 시점의 환불정책 스냅샷
     * @param refund        환불 내역. 환불이 없었으면(결제 전 취소 포함) null.
     *                      취소 요청의 응답에서는 환불 처리 전이라 null이므로 CancelResponse의 금액을 쓴다
     */
    public record ReservationResponse(
            Long id,
            String reservationNo,
            Long accommodationId,
            String accommodationName,
            Long roomId,
            String roomName,
            LocalDate checkIn,
            LocalDate checkOut,
            int guestCount,
            String guestName,
            String guestPhone,
            BigDecimal totalPrice,
            String status,
            LocalDateTime holdExpiresAt,
            LocalDateTime cancelledAt,
            LocalDateTime createdAt,
            List<PriceItemResponse> priceItems,
            JsonNode refundPolicy,
            RefundInfo refund
    ) {
    }

    public record HostReservationSummary(
            Long id,
            String reservationNo,
            Long accommodationId,
            String accommodationName,
            Long roomId,
            String roomName,
            LocalDate checkIn,
            LocalDate checkOut,
            int guestCount,
            String guestName,
            BigDecimal totalPrice,
            String status,
            LocalDateTime createdAt
    ) {
        public static HostReservationSummary of(Reservation r, RoomInfo room) {
            return new HostReservationSummary(r.getId(), r.getReservationNo(), room.accommodationId(),
                    room.accommodationName(), r.getRoomId(), room.roomName(), r.getCheckIn(), r.getCheckOut(),
                    r.getGuestCount(), r.getGuestName(), r.getTotalPrice(), r.getStatus().name(), r.getCreatedAt());

        }

    }
}
