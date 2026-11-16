package com.feature.common.util;

import java.security.SecureRandom;
import java.util.Base64;

// 인증번호와 가입 인증용 랜덤 토큰을 생성합니다.
public final class TokenUtil {
    private static final SecureRandom RANDOM = new SecureRandom();
    private TokenUtil() {}

    // 가입 인증 증명에 사용할 예측하기 어려운 랜덤 토큰을 만듭니다.
    public static String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    // 앞자리 0을 포함할 수 있는 6자리 숫자 인증번호를 만듭니다.
    public static String verificationCode() {
        return String.format(java.util.Locale.ROOT, "%06d", RANDOM.nextInt(1_000_000));
    }
}
