
package com.preen.preen_backend.payment.controller;

import com.preen.preen_backend.common.response.ApiResponse;
import com.preen.preen_backend.payment.dto.TestPaymentResponse;
import com.preen.preen_backend.payment.service.TestPaymentService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.context.annotation.Profile;

@RestController
@Profile("local")
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class TestPaymentController {

    private final TestPaymentService testPaymentService;

    // 테스트 결제 생성
    @PostMapping("/test")
    public ApiResponse<TestPaymentResponse> createTestPayment() {
        return ApiResponse.ok(
                testPaymentService.createTestPayment()
        );
    }

    // 테스트 결제 정보 조회
    @GetMapping("/{paymentId}")
    public ApiResponse<TestPaymentResponse> getTestPayment(
            @PathVariable Long paymentId
    ) {
        return ApiResponse.ok(
                testPaymentService.getTestPayment(paymentId)
        );
    }

    // 테스트 입금 완료 처리
    @PostMapping("/{paymentId}/complete")
    public ApiResponse<TestPaymentResponse> completeTestPayment(
            @PathVariable Long paymentId
    ) {
        return ApiResponse.ok(
                testPaymentService.completeTestPayment(paymentId)
        );
    }
}
