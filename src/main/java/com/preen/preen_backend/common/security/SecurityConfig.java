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

/** API 접근 권한과 CORS 등 공통 보안 규칙을 설정합니다. */
@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,

                                            List<SecurityRuleContributor> contributors, SecurityProperties properties,
                                            ObjectProvider<JwtDecoder> decoder, CurrentSessionLookup sessions, ObjectMapper mapper) throws Exception {

        String[] publicPostPaths = collect(contributors, SecurityRuleContributor::publicPostPaths);
        String[] publicGetPaths = collect(contributors, SecurityRuleContributor::publicGetPaths);
        RequestMatcher[] publicPosts = Arrays.stream(publicPostPaths)
                .map(path -> PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, path))
                .toArray(RequestMatcher[]::new);
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(
                new OrRequestMatcher(List.of(publicPosts)), decoder, sessions, mapper);

        RequestMatcher logoutPost = PathPatternRequestMatcher.withDefaults()
                .matcher(HttpMethod.POST, "/api/auth/logout");

        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(properties.allowedOrigins());
        cors.setAllowCredentials(true);
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cors);


        return http
                .cors(config -> config.configurationSource(source))
                .csrf(config -> config.ignoringRequestMatchers(publicPosts)
                        .ignoringRequestMatchers(logoutPost, request -> SecurityContextHolder.getContext()
                                .getAuthentication() instanceof UsernamePasswordAuthenticationToken token
                                && token.isAuthenticated() && token.getPrincipal() instanceof AuthenticatedUser))

                .sessionManagement(config -> config.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtFilter, CsrfFilter.class)
                .authorizeHttpRequests(config -> config
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
