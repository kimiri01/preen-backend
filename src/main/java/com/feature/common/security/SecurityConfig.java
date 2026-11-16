package com.feature.common.security;

import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

// API 접근 권한과 CORS 등 공통 보안 규칙을 설정합니다.
@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {
    // 공개 API와 인증이 필요한 요청을 구분하고 CORS·세션·CSRF 정책을 적용합니다.
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
            List<SecurityRuleContributor> contributors, SecurityProperties properties) throws Exception {
        // 각 기능에서 등록한 공개 API 경로를 모읍니다.
        String[] publicPaths = contributors.stream()
                .flatMap(rule -> java.util.Arrays.stream(rule.publicPostPaths()))
                .toArray(String[]::new);
        // 허용한 프론트 주소에서 쿠키를 포함한 요청을 보낼 수 있도록 설정합니다.
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(properties.allowedOrigins());
        cors.setAllowCredentials(true); 
        cors.setAllowedMethods(List.of("POST", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        // 서버 세션을 만들지 않고 공개 POST 요청 이외에는 인증을 요구합니다.
        return http
                .cors(config -> config.configurationSource(source))
                .csrf(config -> config.ignoringRequestMatchers(publicPaths))
                .sessionManagement(config -> config.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(config -> config
                        .requestMatchers(HttpMethod.POST, publicPaths).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(config -> config
                        .authenticationEntryPoint((request, response, exception) -> response.sendError(401)))
                .build();
    }
}
