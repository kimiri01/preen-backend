package com.preen.preen_backend.accommodation.controller;


import com.preen.preen_backend.accommodation.service.HostAccommodationService;
import com.preen.preen_backend.common.auth.temporary.AuthUser;
import com.preen.preen_backend.common.auth.temporary.CurrentUser;
import com.preen.preen_backend.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.preen.preen_backend.accommodation.dto.HostAccommodationDtos.*;



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



}
