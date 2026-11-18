
package com.preen.preen_backend.payment.dto;

import java.time.OffsetDateTime;

public record TestPaymentResponse(
        Long paymentId,
        String bankName,
        String accountNumber,
        int amount,
        String status,
        OffsetDateTime expiresAt
) {
}
