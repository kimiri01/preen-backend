package com.preen.preen_backend.common.auth.temporary;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * [개발용 임시 인증] 모든 요청을 임시 사용자(userId 1)로 인증된 것처럼 처리합니다.
 * app.temp-auth.enabled=true 일 때만 등록됩니다. 실제 인증이 붙으면 이 클래스와 설정을 삭제하세요.
 */
public class TempAuthFilter extends OncePerRequestFilter {

    private static final Long TEMP_USER_ID = 1L;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    TEMP_USER_ID, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        }
        chain.doFilter(request, response);
    }
}
