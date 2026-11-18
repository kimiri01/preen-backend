

package com.preen.preen_backend.payment.service;

import com.preen.preen_backend.payment.dto.TestPaymentResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class TestPaymentService {

    // 테스트 결제번호 생성
    private final AtomicLong paymentIdSequence = new AtomicLong(1);

    // 서버 메모리에 테스트 결제 정보 저장
    private final ConcurrentHashMap<Long, TestPaymentResponse> payments =
            new ConcurrentHashMap<>();

    // 테스트 결제 생성
    public TestPaymentResponse createTestPayment() {
        long paymentId = paymentIdSequence.getAndIncrement();

        OffsetDateTime expiresAt =
                OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(10);

        TestPaymentResponse payment = new TestPaymentResponse(
                paymentId,
                "예시은행",
                "123-456-789012",
                50000,
                "WAITING",
                expiresAt
        );

        payments.put(paymentId, payment);

        return payment;
    }

    // 테스트 결제 정보 조회
    public TestPaymentResponse getTestPayment(Long paymentId) {
        return payments.compute(paymentId, (id, payment) -> {
            if (payment == null) {
                throw new ResponseStatusException(
                        NOT_FOUND,
                        "결제 정보를 찾을 수 없습니다."
                );
            }

            if ("WAITING".equals(payment.status())
                    && !OffsetDateTime.now(ZoneOffset.UTC)
                    .isBefore(payment.expiresAt())) {

                return new TestPaymentResponse(
                        payment.paymentId(),
                        payment.bankName(),
                        payment.accountNumber(),
                        payment.amount(),
                        "EXPIRED",
                        payment.expiresAt()
                );
            }

            return payment;
        });
    }

    // 테스트 입금 완료 처리
    public TestPaymentResponse completeTestPayment(Long paymentId) {
        return payments.compute(paymentId, (id, payment) -> {
            if (payment == null) {
                throw new ResponseStatusException(
                        NOT_FOUND,
                        "결제 정보를 찾을 수 없습니다."
                );
            }

            if (!"WAITING".equals(payment.status())) {
                throw new ResponseStatusException(
                        BAD_REQUEST,
                        "결제 대기 중인 경우에만 완료할 수 있습니다."
                );
            }

            if (!OffsetDateTime.now(ZoneOffset.UTC)
                    .isBefore(payment.expiresAt())) {
                return new TestPaymentResponse(
                        payment.paymentId(),
                        payment.bankName(),
                        payment.accountNumber(),
                        payment.amount(),
                        "EXPIRED",
                        payment.expiresAt()
                );
            }

            return new TestPaymentResponse(
                    payment.paymentId(),
                    payment.bankName(),
                    payment.accountNumber(),
                    payment.amount(),
                    "COMPLETED",
                    payment.expiresAt()
            );
        });
    }
}
