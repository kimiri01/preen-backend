package com.preen.preen_backend.reservation.controller;

import com.preen.preen_backend.common.auth.temporary.AuthUser;
import com.preen.preen_backend.common.auth.temporary.CurrentUser;
import com.preen.preen_backend.common.response.ApiResponse;
import com.preen.preen_backend.common.response.PageResponse;
import com.preen.preen_backend.reservation.dto.ReservationDtos.*;
import com.preen.preen_backend.reservation.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    /**
     * 예약 생성 (PENDING + 재고 선점). 응답의 holdExpiresAt까지 결제해야 한다.
     */
    @PostMapping("/api/reservations")
    public ApiResponse<ReservationResponse> create(@CurrentUser AuthUser authUser,
                                                   @Valid @RequestBody CreateRequest request) {
        return ApiResponse.ok(reservationService.create(authUser.userId(), request));
    }

    @GetMapping("/api/reservations/me")
    public ApiResponse<PageResponse<ReservationResponse>> getMine(@CurrentUser AuthUser authUser,
                                                                  @RequestParam(defaultValue = "0") int page,
                                                                  @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(reservationService.getMine(authUser.userId(), page, size));
    }


    @GetMapping("/api/reservations/{id}")
    public ApiResponse<ReservationResponse> getDetail(@CurrentUser AuthUser authUser,
                                                      @PathVariable Long id) {
        return ApiResponse.ok(reservationService.getDetail(authUser.userId(), id));
    }


    /** 호스트 본인 숙소의 예약 목록. HOST 전용. */
    @GetMapping("/api/host/reservations")
    //@ModelAttribute를 쓰면 쿼리 파라미터 이름이 record 필드 이름과 같을 때 자동으로 채워집니다.

    public ApiResponse<PageResponse<HostReservationSummary>> getForHost(@CurrentUser AuthUser authUser,
                                                                     @ModelAttribute HostReservationSearch search,
                                                                     @RequestParam(defaultValue = "0") int page,
                                                                     @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(reservationService.getForHost(authUser.userId(), search, page, size));
    }


}