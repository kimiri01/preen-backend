package com.feature.auth.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

// 인증 유효 시간, 재발송 간격, 해시 비밀키 등 설정값을 읽어옵니다.
@Validated
@ConfigurationProperties("app.auth")
public record AuthProperties(
        Duration codeExpiration,
        Duration resendInterval,
        @Min(1) int maxAttempts,
        Duration signupTokenExpiration,
        @Size(min = 32) String hashSecret,
        @NotBlank String mailFrom) {}
