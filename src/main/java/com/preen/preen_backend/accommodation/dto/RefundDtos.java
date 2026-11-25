package com.preen.preen_backend.accommodation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public final class RefundDtos {

    private RefundDtos() {
    }


    public record RefundPolicyRequest(
            @Size(max = 5000) String policyText,
            @NotEmpty(message = "환불 규칙을 한 개 이상 입력해 주세요.") @Size(max = 20)
            List<@Valid @NotNull RefundRule> rules
    ){
    }


    public record RefundPolicyResponse(Long id,
                                       String version,
                                       String policyText,
                                       List<RefundRule> rules) {
    }


    /**
     * 체크인 daysBefore일 전까지 취소하면 refundRate만큼 환불한다.
     *
     * @param refundRate 0.00(환불 없음) ~ 1.00(전액)
     */
    public record RefundRule(
            @NotNull @Min(value = 0, message = "일수는 0 이상이어야 합니다.") Integer daysBefore,
            @NotNull @DecimalMin(value = "0", message = "환불 비율은 0~1 사이여야 합니다.")
            @DecimalMax(value = "1", message = "환불 비율은 0~1 사이여야 합니다.")
            @Digits(integer = 1, fraction = 2, message = "환불 비율은 소수 둘째 자리까지 입력합니다.")
            BigDecimal refundRate
    ) {
    }

}
