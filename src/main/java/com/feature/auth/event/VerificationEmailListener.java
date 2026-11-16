package com.feature.auth.event;

import lombok.RequiredArgsConstructor;
import com.feature.auth.service.EmailService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

// 인증메일 발송 이벤트를 받아 메일 서비스에 전달합니다.
@Component
@RequiredArgsConstructor
public class VerificationEmailListener {
    private final EmailService emailService;

    // 발송 실패를 요청에 반영하고 DB 갱신을 롤백하기 위해 동기로 처리합니다.
    @EventListener
    public void send(VerificationEmailEvent event) {
        emailService.sendVerification(event.email(), event.code());
    }
}
