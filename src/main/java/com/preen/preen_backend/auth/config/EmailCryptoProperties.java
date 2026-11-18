package com.preen.preen_backend.auth.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

// 이메일 암호화에 사용할 키 설정값을 읽고 형식을 검증합니다.
@Validated
@ConfigurationProperties("app.auth.crypto")
public record EmailCryptoProperties(@NotBlank @Pattern(regexp = "[0-9a-fA-F]{64}") String encryptionKey) {}
