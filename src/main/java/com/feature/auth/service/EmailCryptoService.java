package com.feature.auth.service;

import com.feature.auth.config.EmailCryptoProperties;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// 저장할 이메일을 AES-GCM으로 암호화하고 복호화합니다.
@Service
@RequiredArgsConstructor
public class EmailCryptoService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final EmailCryptoProperties properties;

    // 이메일 원문을 암호화하여 DB에 저장할 문자열로 변환합니다.
    public String encrypt(String email) {
        byte[] iv = new byte[12];
        RANDOM.nextBytes(iv);
        try {
            Cipher cipher = cipher(Cipher.ENCRYPT_MODE, iv);
            byte[] encrypted = cipher.doFinal(email.getBytes(StandardCharsets.UTF_8));
            return "v1:" + Base64.getEncoder().encodeToString(ByteBuffer
                    .allocate(iv.length + encrypted.length).put(iv).put(encrypted).array());
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("이메일 암호화에 실패했습니다.", exception);
        }
    }

    // 저장된 이메일 암호문을 원래 주소로 복원합니다.
    public String decrypt(String encryptedEmail) {
        if (!encryptedEmail.startsWith("v1:"))
            throw new IllegalArgumentException("지원하지 않는 암호문입니다.");

        byte[] payload = Base64.getDecoder().decode(encryptedEmail.substring(3));
        if (payload.length < 28)
            throw new IllegalArgumentException("잘못된 암호문입니다.");
        ByteBuffer buffer = ByteBuffer.wrap(payload);
        byte[] iv = new byte[12];
        buffer.get(iv);
        byte[] encrypted = new byte[buffer.remaining()];
        buffer.get(encrypted);

        try {
            return new String(cipher(Cipher.DECRYPT_MODE, iv).doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("이메일 복호화에 실패했습니다.", exception);
        }
    }

    // 이메일 전용 부가 인증 데이터를 적용한 AES-GCM 암호화기를 구성합니다.
    private Cipher cipher(int mode, byte[] iv) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(mode, new SecretKeySpec(HexFormat.of().parseHex(properties.encryptionKey()), "AES"),
                new GCMParameterSpec(128, iv));
        cipher.updateAAD("preen:email:v1".getBytes(StandardCharsets.UTF_8));
        return cipher;
    }
}
