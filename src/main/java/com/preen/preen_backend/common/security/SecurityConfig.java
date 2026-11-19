package com.preen.preen_backend.common.security;

import jakarta.servlet.DispatcherType;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

/**
 * API 접근 권한과 CORS 등 공통 보안 규칙을 설정합니다.
 *
 * CORS: 프론트엔드(Vite 개발 서버, http://localhost:5173)와 백엔드(http://localhost:8080)는
 * 주소(포트)가 달라서, 백엔드가 허용해 주지 않으면 브라우저가 응답을 막습니다.
 * (Spring Security를 쓰면 CORS도 여기서 처리해야 해서, 기존 WebConfig 내용을 이곳으로 옮겼습니다.)
 */
@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            List<SecurityRuleContributor> contributors, SecurityProperties properties,
                                            ObjectProvider<JwtDecoder> decoder, CurrentSessionLookup sessions, ObjectMapper mapper) throws Exception {
        // 각 기능에서 등록한 공개 API 경로를 모읍니다.
        String[] publicPostPaths = collect(contributors, SecurityRuleContributor::publicPostPaths);
        String[] publicGetPaths = collect(contributors, SecurityRuleContributor::publicGetPaths);
        RequestMatcher[] publicPosts = Arrays.stream(publicPostPaths)
                .map(path -> PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, path))
                .toArray(RequestMatcher[]::new);
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(
                new OrRequestMatcher(List.of(publicPosts)), decoder, sessions, mapper);

        RequestMatcher logoutPost = PathPatternRequestMatcher.withDefaults()
                .matcher(HttpMethod.POST, "/api/auth/logout");

        // 허용한 프론트 주소에서 쿠키를 포함한 요청을 보낼 수 있도록 설정합니다.
        // 프론트 공통 http 모듈이 credentials: 'include'로 요청하므로 allowCredentials는 true여야 합니다.
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(properties.allowedOrigins());
        cors.setAllowCredentials(true);
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cors);

        // 서버 세션을 만들지 않고, 공개 경로 이외에는 인증을 요구합니다.
        return http
                .cors(config -> config.configurationSource(source))
                // 로그인·재발급·로그아웃의 쿠키 요청은 인증 API에서 Origin을 확인합니다.
                // 나머지 보호 API는 직접 보낸 Bearer 토큰으로 인증한 경우 CSRF 토큰을 요구하지 않습니다.
                .csrf(config -> config.ignoringRequestMatchers(publicPosts)
                        .ignoringRequestMatchers(logoutPost, request -> SecurityContextHolder.getContext()
                                .getAuthentication() instanceof UsernamePasswordAuthenticationToken token
                                && token.isAuthenticated() && token.getPrincipal() instanceof AuthenticatedUser))
                .sessionManagement(config -> config.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtFilter, CsrfFilter.class)
                .authorizeHttpRequests(config -> config
                        // 400·404 같은 에러 응답을 만들 때 내부적으로 /error로 넘어가는데, 이것까지 막으면 전부 401이 됩니다.
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, publicGetPaths).permitAll()
                        .requestMatchers(HttpMethod.POST, publicPostPaths).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(config -> config
                        .authenticationEntryPoint((request, response, exception) ->
                                SecurityErrorCode.SESSION_INVALID.write(response, mapper))
                        .accessDeniedHandler((request, response, exception) ->
                                SecurityErrorCode.ACCESS_DENIED.write(response, mapper)))
                .build();
    }

    private static String[] collect(List<SecurityRuleContributor> contributors,
            Function<SecurityRuleContributor, String[]> paths) {
        return contributors.stream()
                .flatMap(rule -> Arrays.stream(paths.apply(rule)))
                .toArray(String[]::new);
    }
}
