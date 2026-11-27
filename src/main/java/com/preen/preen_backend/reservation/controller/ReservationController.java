package com.preen.preen_backend.reservation.controller;


import com.preen.preen_backend.common.response.ApiResponse;
import com.preen.preen_backend.common.response.PageResponse;
import com.preen.preen_backend.common.security.AuthenticatedUser;
import com.preen.preen_backend.reservation.domain.Reservation.Status;
import com.preen.preen_backend.reservation.dto.ReservationDtos.CreateRequest;
import com.preen.preen_backend.reservation.dto.ReservationDtos.ReservationResponse;
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
    public ApiResponse<ReservationResponse> create(@AuthenticationPrincipal AuthenticatedUser authUser,
                                                   @Valid @RequestBody CreateRequest request) {
        return ApiResponse.ok(reservationService.create(authUser.userId(), request));
    }

    @GetMapping("/api/reservations/me")
    public ApiResponse<PageResponse<ReservationResponse>> getMine(@AuthenticationPrincipal AuthenticatedUser authUser,
                                                                  @RequestParam(required = false) Status status,
                                                                  @RequestParam(defaultValue = "0") int page,
                                                                  @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(reservationService.getMine(authUser.userId(), status, page, size));
    }


    @GetMapping("/api/reservations/{id}")
    public ApiResponse<ReservationResponse> getDetail(@AuthenticationPrincipal AuthenticatedUser authUser,
                                                      @PathVariable Long id) {
        return ApiResponse.ok(reservationService.getDetail(authUser.userId(), id));
    }


}