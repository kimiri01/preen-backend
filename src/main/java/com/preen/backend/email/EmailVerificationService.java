
package com.preen.backend.email;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
public class EmailVerificationService {

    private final EmailVerificationRepository repository;
    private final JavaMailSender mailSender;
    private final SecureRandom random = new SecureRandom();

    public EmailVerificationService(
            EmailVerificationRepository repository,
            JavaMailSender mailSender
    ) {
        this.repository = repository;
        this.mailSender = mailSender;
    }

    @Transactional
    public void sendVerificationCode(String email) {

        String code = String.format(
                "%06d",
                random.nextInt(1_000_000)
        );

        EmailVerification verification =
                new EmailVerification();

        verification.setEmail(email);
        verification.setVerificationCode(hashCode(code));
        verification.setExpiresAt(
                LocalDateTime.now().plusMinutes(5)
        );
        verification.setVerified(false);

        repository.save(verification);

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setTo(email);
        message.setSubject("[PREEN] 이메일 인증번호");
        message.setText(
                "안녕하세요, PREEN입니다.\n\n"
                        + "이메일 인증번호는 "
                        + code
                        + " 입니다.\n\n"
                        + "인증번호는 5분 동안 유효합니다."
        );

        mailSender.send(message);
    }

    private String hashCode(String code) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hashed = digest.digest(
                    code.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hashed);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "인증번호 해시 생성 실패", e
            );
        }
    }

    @Transactional
    public boolean verifyCode(String email, String code) {

        EmailVerification verification = repository
                .findTopByEmailOrderByCreatedAtDesc(email)
                .orElse(null);

        if (verification == null) {
            return false;
        }

        if (verification.isVerified()) {
            return false;
        }

        if (LocalDateTime.now().isAfter(
                verification.getExpiresAt())) {
            return false;
        }

        String hashedInput = hashCode(code);

        boolean matches = MessageDigest.isEqual(
                hashedInput.getBytes(StandardCharsets.UTF_8),
                verification.getVerificationCode()
                        .getBytes(StandardCharsets.UTF_8)
        );

        if (!matches) {
            return false;
        }

        verification.setVerified(true);
        repository.save(verification);

        return true;
    }
}