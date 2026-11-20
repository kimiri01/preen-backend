package com.preen.preen_backend.myPage.controller;

import com.preen.preen_backend.common.response.ApiResponse;
import com.preen.preen_backend.myPage.dto.MyInfoResponse;
import com.preen.preen_backend.myPage.dto.MyInfoUpdateRequest;
import com.preen.preen_backend.myPage.service.MyInfoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MyInfoControllerTest {

    @Mock
    private MyInfoService myInfoService;

    @InjectMocks
    private MyInfoController controller;

    @Test
    void 인증된_회원은_내_정보를_조회할_수_있다() {
        MyInfoResponse expected = new MyInfoResponse(
                "홍길동",
                "test@example.com",
                LocalDate.of(2000, 1, 1)
        );

        when(myInfoService.getMyInfo(1L)).thenReturn(expected);

        ResponseEntity<ApiResponse<MyInfoResponse>> response =
                controller.getMyInfo(1L);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().success());
        assertEquals(expected, response.getBody().data());

        verify(myInfoService).getMyInfo(1L);
    }

    @Test
    void 인증된_회원은_내_정보를_수정할_수_있다() {
        MyInfoUpdateRequest request = new MyInfoUpdateRequest(
                "김철수",
                LocalDate.of(1999, 5, 10)
        );

        MyInfoResponse expected = new MyInfoResponse(
                "김철수",
                "test@example.com",
                LocalDate.of(1999, 5, 10)
        );

        when(myInfoService.updateMyInfo(1L, request))
                .thenReturn(expected);

        ResponseEntity<ApiResponse<MyInfoResponse>> response =
                controller.updateMyInfo(1L, request);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(expected, response.getBody().data());

        verify(myInfoService).updateMyInfo(1L, request);
    }

    @Test
    void 회원_ID가_없으면_조회할_수_없다() {
        assertThrows(
                InsufficientAuthenticationException.class,
                () -> controller.getMyInfo(null)
        );

        verifyNoInteractions(myInfoService);
    }

    @Test
    void 잘못된_회원_ID로는_수정할_수_없다() {
        MyInfoUpdateRequest request = new MyInfoUpdateRequest(
                "김철수",
                LocalDate.of(1999, 5, 10)
        );

        assertThrows(
                InsufficientAuthenticationException.class,
                () -> controller.updateMyInfo(0L, request)
        );

        verifyNoInteractions(myInfoService);
    }
}