package com.preen.preen_backend.inventory.controller;


import com.preen.preen_backend.common.auth.temporary.AuthUser;
import com.preen.preen_backend.common.auth.temporary.CurrentUser;
import com.preen.preen_backend.common.response.ApiResponse;
import com.preen.preen_backend.inventory.dto.InventoryDtos;
import com.preen.preen_backend.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.preen.preen_backend.inventory.dto.InventoryDtos.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;


    /**
     * 기간별 재고 조회 (양 끝 포함, 최대 366일). 날짜별 수량, 예약·선점 수, 가격, 판매 마감 여부.
     * HOST 전용이며 본인 객실인지 서비스에서 검사한다. 날짜는 yyyy-MM-dd.
     */
    @GetMapping("/api/host/rooms/{roomId}/inventories")
    public ApiResponse<List<InventoryResponse>> getInventories(
            @CurrentUser AuthUser authUser,
            @PathVariable Long roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.ok(inventoryService.getInventories(authUser.userId(), roomId, startDate, endDate));
    }


    /** 기간별 수량, 가격, 판매 마감 설정. HOST 전용이며 본인 객실인지 서비스에서 검사한다. */
    @PutMapping("/api/host/rooms/{roomId}/inventories")
    public ApiResponse<List<InventoryResponse>> setInventories(@CurrentUser AuthUser authUser,
                                                               @PathVariable Long roomId,
                                                               @Valid @RequestBody SetInventoriesRequest request) {
        return ApiResponse.ok(inventoryService.setInventories(authUser.userId(), roomId, request));
    }




}
