package com.preen.preen_backend.auth.service;

import com.preen.preen_backend.auth.dto.CurrentMemberResponse;
import com.preen.preen_backend.auth.entity.User;
import com.preen.preen_backend.auth.entity.UserPersonalInfo;
import com.preen.preen_backend.auth.exception.AuthErrorCode;
import com.preen.preen_backend.auth.exception.LoginRejectedException;
import com.preen.preen_backend.auth.repository.RedisLoginSessionRepository;
import com.preen.preen_backend.auth.repository.RedisLoginSessionRepository.LoginSession;
import com.preen.preen_backend.auth.repository.UserPersonalInfoRepository;
import com.preen.preen_backend.auth.repository.UserRepository;
import com.preen.preen_backend.common.exception.BusinessException;
import com.preen.preen_backend.common.security.AccessTokenIssuer;
import com.preen.preen_backend.common.security.AuthenticatedUser;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoginSessionService {
    private static final Pattern REFRESH_TOKEN = Pattern.compile(
            "([1-9][0-9]{0,18})\\.([a-f0-9-]{36})\\.(?:(s|p)\\.)?[A-Za-z0-9_-]{43}");
    private static final int MAX_REFRESH_LENGTH = 102;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final LoginAccountService accounts;
    private final RedisLoginSessionRepository sessions;
    private final AuthHashService hashes;
    private final UserRepository users;
    private final UserPersonalInfoRepository personalInfo;
    private final ObjectProvider<AccessTokenIssuer> tokenIssuer;
    private final Clock clock;

    // 비밀번호가 맞으면 새 세션과 토큰을 만들고 기존 세션을 교체합니다.
    // 세션을 저장할 때까지 회원 인증 정보의 잠금을 유지합니다.
    @Transactional(noRollbackFor = LoginRejectedException.class)
    public SessionTokens login(String email, String password, boolean rememberMe) {
        User user = accounts.authenticate(email, password);
        String sessionId = UUID.randomUUID().toString();
        String refreshToken = newRefreshToken(user.getId(), sessionId, rememberMe);
        String accessToken = tokenIssuer.getObject().issue(user.getId(), user.getRole(), sessionId);
        try {
            Instant expiresAt = sessions.save(user.getId(),
                    new LoginSession(sessionId, refreshHash(refreshToken), clock.instant()));
            return new SessionTokens(accessToken, refreshToken, expiresAt, rememberMe);
        } catch (DataAccessException exception) {
            throw new BusinessException(AuthErrorCode.SESSION_LOOKUP_UNAVAILABLE);
        }
    }

    // 쿠키에서 회원과 세션을 찾고, 이전 쿠키가 유효할 때만 새 쿠키로 교체합니다.
    public SessionTokens refresh(String currentToken) {
        if (currentToken == null || currentToken.length() > MAX_REFRESH_LENGTH) {
            throw invalidSession();
        }
        var matcher = REFRESH_TOKEN.matcher(currentToken);
        if (!matcher.matches()) {
            throw invalidSession();
        }
        long userId;
        try {
            userId = Long.parseLong(matcher.group(1));
        } catch (NumberFormatException exception) {
            throw invalidSession();
        }
        String sessionId = matcher.group(2);
        // 쿠키 유형도 토큰 해시로 검증되므로 재발급해도 처음 선택이 유지됩니다.
        boolean rememberMe = !"s".equals(matcher.group(3));
        User user = activeUser(userId);
        String refreshToken = newRefreshToken(userId, sessionId, rememberMe);
        String accessToken = tokenIssuer.getObject().issue(userId, user.getRole(), sessionId);
        try {
            Instant expiresAt = sessions.rotateRefreshToken(
                    userId, sessionId, refreshHash(currentToken), refreshHash(refreshToken))
                    .orElseThrow(LoginSessionService::invalidSession);
            return new SessionTokens(accessToken, refreshToken, expiresAt, rememberMe);
        } catch (DataAccessException exception) {
            throw new BusinessException(AuthErrorCode.SESSION_LOOKUP_UNAVAILABLE);
        }
    }

    // 로그인한 회원의 최신 이름과 권한을 반환합니다.
    public CurrentMemberResponse currentMember(AuthenticatedUser principal) {
        User user = activeUser(principal.userId());
        String name = personalInfo.findById(user.getId()).map(UserPersonalInfo::getName)
                .orElseThrow(LoginSessionService::invalidSession);
        return new CurrentMemberResponse(user.getId(), name, user.getRole());
    }

    // 요청이 확인된 시점에 해당 회원의 현재 세션만 종료합니다.
    public void logout(AuthenticatedUser principal) {
        try {
            if (!sessions.deleteIfCurrent(principal.userId(), principal.sessionId())) {
                throw invalidSession();
            }
        } catch (DataAccessException exception) {
            throw new BusinessException(AuthErrorCode.SESSION_LOOKUP_UNAVAILABLE);
        }
    }

    private User activeUser(long userId) {
        return users.findById(userId).filter(User::isActive)
                .orElseThrow(LoginSessionService::invalidSession);
    }

    private static String newRefreshToken(long userId, String sessionId, boolean rememberMe) {
        byte[] secret = new byte[32];
        RANDOM.nextBytes(secret);
        String cookieType = rememberMe ? "p" : "s";
        return userId + "." + sessionId + "." + cookieType + "."
                + Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
    }

    private String refreshHash(String token) {
        return hashes.hash("refresh:" + token);
    }

    private static BusinessException invalidSession() {
        return new BusinessException(AuthErrorCode.SESSION_INVALID);
    }

    public record SessionTokens(String accessToken, String refreshToken, Instant expiresAt, boolean rememberMe) {
        @Override
        public String toString() {
            return "SessionTokens[redacted]";
        }
    }
}
