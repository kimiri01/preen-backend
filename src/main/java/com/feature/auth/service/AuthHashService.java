package com.feature.auth.service;

import lombok.RequiredArgsConstructor;
import com.feature.auth.config.AuthProperties;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

// 이메일·인증번호·가입 토큰의 조회 및 검증용 HMAC 해시를 처리합니다.
@Service
@RequiredArgsConstructor
public class AuthHashService {
    private final AuthProperties properties;

    // 비밀키 기반 HMAC-SHA256 해시를 생성합니다.
    public String hash(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.hashSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("인증 해시 초기화에 실패했습니다.", exception);
        }
    }
    // 입력값의 해시가 저장된 해시와 일치하는지 확인합니다.
    public boolean matches(String value, String expected) {
        return MessageDigest.isEqual(hash(value).getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8));
    }
}
