package com.preen.preen_backend.host.controller;


import com.preen.preen_backend.common.auth.temporary.AuthUser;
import com.preen.preen_backend.common.auth.temporary.CurrentUser;
import com.preen.preen_backend.common.response.ApiResponse;
import com.preen.preen_backend.host.dto.HostDtos;
import com.preen.preen_backend.host.service.HostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;



@RestController
@RequiredArgsConstructor


public class HostBusinessController {


    private final HostService hostService;


    /** 로그인한 회원이면 누구나 호출한다 (/api/host/** 가 아니므로 GUEST도 접근 가능). */
    @PostMapping("/api/host-businesses")
    public ApiResponse<HostDtos.RegisterResponse> register(@CurrentUser AuthUser authUser,
                                                           @Valid @RequestBody HostDtos.RegisterRequest request){
        return ApiResponse.ok(hostService.register(authUser.userId(), request));



    }


}
