package com.preen.preen_backend.common.util;

import java.util.Locale;

// 이메일 주소의 공백과 대소문자를 정리합니다.
public final class EmailUtil {
    private EmailUtil() {}

    public static String normalize(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
