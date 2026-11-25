package com.preen.preen_backend.accommodation.controller;


import com.preen.preen_backend.accommodation.domain.RefundPolicy;
import com.preen.preen_backend.accommodation.dto.RefundDtos.*;
import com.preen.preen_backend.accommodation.service.HostAccommodationService;
import com.preen.preen_backend.common.auth.temporary.AuthUser;
import com.preen.preen_backend.common.auth.temporary.CurrentUser;
import com.preen.preen_backend.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.preen.preen_backend.accommodation.dto.HostAccommodationDtos.*;
import com.preen.preen_backend.accommodation.dto.RefundDtos.*;



@RestController
@RequestMapping("/api/host/accommodations")
@RequiredArgsConstructor
public class HostAccommodationController {

    private final HostAccommodationService hostAccommodationService;


    @PostMapping
    public ApiResponse<HostAccommodationDetail> create(@CurrentUser AuthUser authUser,
                                                   @Valid @RequestBody AccommodationCreateRequest request) {
        return ApiResponse.ok(hostAccommodationService.create(authUser.userId(), request));
    }

    /** 내 숙소 목록. 공개 상태(DRAFT·LISTED·HIDDEN)와 관계없이 모두 나온다. */
    @GetMapping
    public ApiResponse<List<HostAccommodationSummary>> getMine(@CurrentUser AuthUser authUser){
        return ApiResponse.ok(hostAccommodationService.getMine(authUser.userId()));
    }


    /** 내 숙소 상세. 공개 전이거나 숨긴 숙소도 보이고, 판매 중지 객실도 포함한다. */
    @GetMapping("/{id}")
    public ApiResponse<HostAccommodationDetail> getMyDetail(@CurrentUser AuthUser authUser,
                                                        @PathVariable Long id) {
        return ApiResponse.ok(hostAccommodationService.getMyDetail(authUser.userId(), id));
    }


    /** 숙소 세부사항 수정. 공개 상태는 바꾸지 않는다. */
    @PatchMapping("/{id}")
    public ApiResponse<HostAccommodationDetail> update(@CurrentUser AuthUser authUser,
                                                   @PathVariable Long id,
                                                   @Valid @RequestBody AccommodationUpdateRequest request) {
        return ApiResponse.ok(hostAccommodationService.update(authUser.userId(), id, request));
    }

    //객실 등록
    @PostMapping("/{id}/rooms")
    public ApiResponse<RoomResponse> addRoom(@CurrentUser AuthUser authUser,
                                             @PathVariable Long id,
                                             @Valid @RequestBody RoomCreateRequest request) {
        return ApiResponse.ok(hostAccommodationService.addRoom(authUser.userId(), id, request));
    }


    /** 객실 수정·판매 중지. 공개 중인 숙소의 마지막 판매 객실은 중지할 수 없다. */
    @PatchMapping("/{id}/rooms/{roomId}")
    public ApiResponse<RoomResponse> updateRoom(@CurrentUser AuthUser authUser,
                                                @PathVariable Long id,
                                                @PathVariable Long roomId,
                                                @Valid @RequestBody RoomUpdateRequest request) {
        return ApiResponse.ok(hostAccommodationService.updateRoom(authUser.userId(), id, roomId, request));
    }

//    @PutMapping("/{id}/refund-policy")
//    public ApiResponse<RefundPolicyResponse> putRefundPolicy(@CurrentUser AuthUser authUser,
//                                                             @PathVariable Long id,
//                                                             @Valid @RequestBody RefundPolicyRequest request){
//        return;// ApiResponse.ok(hostAccommodationService.putRefundPolicy(authUser.userId(), id, request));
//    }



}
