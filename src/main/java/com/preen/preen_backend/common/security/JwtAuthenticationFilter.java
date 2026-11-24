package com.preen.preen_backend.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

@RequiredArgsConstructor
class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final String BEARER = "Bearer ";

    private final RequestMatcher publicPosts;
    private final ObjectProvider<JwtDecoder> decoder;
    private final CurrentSessionLookup sessions;
    private final ObjectMapper mapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return publicPosts.matches(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.regionMatches(true, 0, BEARER, 0, BEARER.length())) {
            chain.doFilter(request, response);
            return;
        }

        long userId;
        String sessionId;
        UserRole role;
        try {
            var jwt = decoder.getObject().decode(header.substring(BEARER.length()));
            userId = Long.parseLong(jwt.getSubject());
            String roleClaim = jwt.getClaimAsString("role");
            sessionId = jwt.getClaimAsString("sid");
            if (userId <= 0 || sessionId == null || sessionId.isBlank() || roleClaim == null) {
                throw new IllegalArgumentException("필수 인증 정보가 유효하지 않습니다.");
            }
            role = UserRole.valueOf(roleClaim);
        } catch (JwtValidationException exception) {
            boolean expired = exception.getErrors().stream()
                    .anyMatch(error -> SecurityErrorCode.ACCESS_TOKEN_EXPIRED.name().equals(error.getErrorCode()));
            (expired ? SecurityErrorCode.ACCESS_TOKEN_EXPIRED : SecurityErrorCode.SESSION_INVALID)
                    .write(response, mapper);
            return;
        } catch (JwtException | IllegalArgumentException exception) {
            SecurityErrorCode.SESSION_INVALID.write(response, mapper);
            return;
        }

        try {
            if (!sessions.isCurrentSession(userId, sessionId)) {
                SecurityErrorCode.SESSION_INVALID.write(response, mapper);
                return;
            }
        } catch (DataAccessException exception) {
            SecurityErrorCode.SESSION_LOOKUP_UNAVAILABLE.write(response, mapper);
            return;
        }

        var principal = new AuthenticatedUser(userId, role, sessionId);
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        chain.doFilter(request, response);
    }
}
