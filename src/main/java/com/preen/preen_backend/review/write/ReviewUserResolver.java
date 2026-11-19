package com.preen.preen_backend.review.write;

import org.springframework.http.HttpStatus;
import com.preen.preen_backend.common.security.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ReviewUserResolver {
    private final boolean temporaryAuthEnabled;

    public ReviewUserResolver(@Value("${app.temp-auth.enabled:false}") boolean temporaryAuthEnabled) {
        this.temporaryAuthEnabled = temporaryAuthEnabled;
    }

    public long requireUserId(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof ReviewUserPrincipal user && user.userId() > 0) {
            return user.userId();
        }
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof AuthenticatedUser user && user.userId() > 0) {
            return user.userId();
        }
        // Match only the server-side principal installed by the team's TempAuthFilter.
        // Never turn this on here, or accept a client userId / numeric username.
        if (temporaryAuthEnabled
                && authentication instanceof UsernamePasswordAuthenticationToken
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof Long userId && userId > 0
                && authentication.getAuthorities().stream().anyMatch(role -> "ROLE_USER".equals(role.getAuthority()))) {
            return userId;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 사용자 확인이 필요합니다.");
    }
}
