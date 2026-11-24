package com.preen.preen_backend.common.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

@Component
@Lazy
@RequiredArgsConstructor
public class AccessTokenIssuer {
    static final String ISSUER = "preen-backend";
    private static final Duration TOKEN_LIFETIME = Duration.ofMinutes(60);

    private final JwtEncoder encoder;
    private final Clock clock;

    // 회원 번호, 권한, 세션 ID를 담고 발급 시각부터 60분간 유효한 토큰에 서명합니다.
    public String issue(long userId, UserRole role, String sessionId) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(Long.toString(userId))
                .issuedAt(now)
                .expiresAt(now.plus(TOKEN_LIFETIME))
                .claim("role", role.name())
                .claim("sid", sessionId)
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
