package com.preen.preen_backend.auth.service;

import lombok.RequiredArgsConstructor;
import com.preen.preen_backend.auth.config.AuthProperties;
import com.preen.preen_backend.auth.exception.AuthErrorCode;
import com.preen.preen_backend.common.exception.BusinessException;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

// SMTP 메일 서버를 통해 인증번호를 발송합니다.
@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender sender;
    private final AuthProperties properties;

    // 인증번호와 유효 시간을 이메일로 발송하고 발송 실패를 업무 오류로 전달합니다.
    public void sendVerification(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.mailFrom());
        message.setTo(email);
        message.setSubject("[PREEN] 회원가입 이메일 인증번호");
        message.setText("인증번호는 " + code + "입니다. "
                + properties.codeExpiration().toMinutes() + "분 이내에 입력해 주세요.");
        try {
            sender.send(message);
        } catch (MailException exception) {
            throw new BusinessException(AuthErrorCode.MAIL_SEND_FAILED);
        }
    }
}
