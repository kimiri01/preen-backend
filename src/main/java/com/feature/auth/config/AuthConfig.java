package com.feature.auth.config;

import java.util.Map;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;

// 인증 설정값과 비밀번호 해시 처리기를 등록합니다.
@Configuration
@EnableConfigurationProperties({AuthProperties.class, EmailCryptoProperties.class})
public class AuthConfig {
    // 회원 비밀번호를 PBKDF2로 해시하고 검증할 처리기를 등록합니다.
    @Bean
    PasswordEncoder passwordEncoder() {
        String algorithm = "pbkdf2@SpringSecurity_v5_8";
        return new DelegatingPasswordEncoder(algorithm,
                Map.of(algorithm, Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8()));
    }
}
