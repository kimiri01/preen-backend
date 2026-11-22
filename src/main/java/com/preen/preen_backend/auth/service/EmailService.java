package com.preen.preen_backend.auth.service;

import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import com.preen.preen_backend.auth.config.AuthProperties;
import com.preen.preen_backend.auth.exception.AuthErrorCode;
import com.preen.preen_backend.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

// SMTP 메일 서버를 통해 인증번호와 비밀번호 재설정 링크를 발송합니다.
@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender sender;
    private final AuthProperties properties;

    @Value("${app.auth.password-reset-page-url}")
    private String passwordResetPageUrl;

    // 인증번호와 유효 시간을 이메일로 발송하고 발송 실패를 업무 오류로 전달합니다.
    public void sendVerification(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.mailFrom());
        message.setTo(email);
        message.setSubject("[PREEN] 회원가입 이메일 인증번호");
        message.setText("인증번호는 " + code + "입니다. "
                + properties.codeExpiration().toMinutes() + "분 이내에 입력해 주세요.");
        send(message);
    }

    // 메일에서 바로 재설정 화면을 열 수 있도록 HTML 링크를 보냅니다.
    public void sendPasswordReset(String email, String token, long expiresInMinutes) {
        String link = HtmlUtils.htmlEscape(passwordResetPageUrl + "?token=" + token);

        try {
            MimeMessageHelper message = new MimeMessageHelper(sender.createMimeMessage(), "UTF-8");
            message.setFrom(properties.mailFrom());
            message.setTo(email);
            message.setSubject("[PREEN] 비밀번호 재설정 안내");
            message.setText("""
                    <p>아래 링크에서 새 비밀번호를 설정해 주세요.</p>
                    <p><a href="%s">비밀번호 재설정</a></p>
                    <p>링크는 %d분 동안 유효합니다.<br>
                    본인이 요청하지 않았다면 이 메일을 무시해 주세요.</p>
                    """.formatted(link, expiresInMinutes), true);
            sender.send(message.getMimeMessage());
        } catch (MessagingException | MailException exception) {
            throw new BusinessException(AuthErrorCode.MAIL_SEND_FAILED);
        }
    }

    private void send(SimpleMailMessage message) {
        try {
            sender.send(message);
        } catch (MailException exception) {
            throw new BusinessException(AuthErrorCode.MAIL_SEND_FAILED);
        }
    }
}
