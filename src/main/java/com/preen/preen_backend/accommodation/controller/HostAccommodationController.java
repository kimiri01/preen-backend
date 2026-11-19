package com.preen.preen_backend.accommodation.controller;


import com.preen.preen_backend.accommodation.dto.HostAccommodationSummary;
import com.preen.preen_backend.accommodation.service.HostAccommodationService;
import com.preen.preen_backend.common.auth.temporary.AuthUser;
import com.preen.preen_backend.common.auth.temporary.CurrentUser;
import com.preen.preen_backend.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/host/accommodations")
@RequiredArgsConstructor
public class HostAccommodationController {

    private final HostAccommodationService hostAccommodationService;

    @GetMapping
    public ApiResponse<List<HostAccommodationSummary>> getMine(@CurrentUser AuthUser authUser){
        return ApiResponse.ok(hostAccommodationService.getMine(authUser.userId()));
    }



}
