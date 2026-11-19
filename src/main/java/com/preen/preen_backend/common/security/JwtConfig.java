package com.preen.preen_backend.common.security;

import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
public class JwtConfig {
    // 환경변수에서 서명키를 읽고 HS256에 필요한 길이인지 확인합니다.
    @Bean
    @Lazy
    SecretKey jwtSigningKey(@Value("${app.security.jwt.signing-key}") String encodedKey) {
        byte[] key;
        try {
            key = Base64.getDecoder().decode(encodedKey);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("JWT_SIGNING_KEY는 Base64 형식이어야 합니다.");
        }
        if (key.length < 32) {
            throw new IllegalStateException("JWT_SIGNING_KEY는 디코딩 후 32바이트 이상이어야 합니다.");
        }
        return new SecretKeySpec(key, "HmacSHA256");
    }

    @Bean
    @Lazy
    JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
        return NimbusJwtEncoder.withSecretKey(jwtSigningKey).algorithm(MacAlgorithm.HS256).build();
    }

    // 발급에 사용한 키로 검증하고, 발급자와 유효 시간을 확인합니다.
    @Bean
    @Lazy
    JwtDecoder jwtDecoder(SecretKey jwtSigningKey, Clock clock) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSigningKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        JwtIssuerValidator issuerValidator = new JwtIssuerValidator(AccessTokenIssuer.ISSUER);
        decoder.setJwtValidator(jwt -> {
            var issuer = issuerValidator.validate(jwt);
            if (issuer.hasErrors()) {
                return issuer;
            }
            Instant now = clock.instant();
            if (jwt.getExpiresAt() == null
                    || (jwt.getNotBefore() != null && now.isBefore(jwt.getNotBefore()))) {
                return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
            }
            if (!now.isBefore(jwt.getExpiresAt())) {
                return OAuth2TokenValidatorResult.failure(
                        new OAuth2Error(SecurityErrorCode.ACCESS_TOKEN_EXPIRED.name()));
            }
            return OAuth2TokenValidatorResult.success();
        });
        return decoder;
    }
}
