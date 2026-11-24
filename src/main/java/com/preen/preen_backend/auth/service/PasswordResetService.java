package com.preen.preen_backend.auth.service;

import com.preen.preen_backend.auth.entity.PasswordResetToken;
import com.preen.preen_backend.auth.entity.User;
import com.preen.preen_backend.auth.entity.UserCredentials;
import com.preen.preen_backend.auth.exception.AuthErrorCode;
import com.preen.preen_backend.auth.repository.PasswordResetTokenRepository;
import com.preen.preen_backend.auth.repository.RedisLoginSessionRepository;
import com.preen.preen_backend.auth.repository.UserCredentialsRepository;
import com.preen.preen_backend.auth.repository.UserPersonalInfoRepository;
import com.preen.preen_backend.auth.repository.UserRepository;
import com.preen.preen_backend.common.exception.BusinessException;
import com.preen.preen_backend.common.util.RateLimiter;
import com.preen.preen_backend.common.util.TokenUtil;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasswordResetService {
    private static final int REQUESTS_PER_EMAIL = 3;
    private static final Duration TOKEN_EXPIRATION = Duration.ofMinutes(15);

    private static final Pattern TOKEN_PATTERN = Pattern.compile("[A-Za-z0-9_-]{43}");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "(?U)(?=.*[A-Za-z])(?=.*[0-9])(?=.*[^A-Za-z0-9\\s])(?!.*\\s).{8,50}");

    private final UserPersonalInfoRepository personalInfo;
    private final UserRepository users;
    private final UserCredentialsRepository credentials;
    private final PasswordResetTokenRepository resetTokens;
    private final AuthHashService hashes;
    private final Clock clock;
    private final EmailService emailService;
    private final RateLimiter rateLimiter;
    private final PasswordEncoder passwordEncoder;
    private final RedisLoginSessionRepository sessions;

    @Transactional
    public void requestLink(String email) {
        String emailHash = hashes.hash(email);
        rateLimiter.check("password-reset-email:" + emailHash, REQUESTS_PER_EMAIL);

        var member = personalInfo.findByEmailHash(emailHash);
        if (member.isEmpty()) {
            return;
        }

        Long userId = member.get().getUserId();
        if (users.findById(userId).filter(User::isActive).isEmpty() || !credentials.existsById(userId)) {
            return;
        }

        String token = TokenUtil.randomToken();
        Instant expiresAt = clock.instant().plus(TOKEN_EXPIRATION);
        resetTokens.save(new PasswordResetToken(userId, hashes.hash(token), expiresAt));
        emailService.sendPasswordReset(email, token, TOKEN_EXPIRATION.toMinutes());
    }

    @Transactional
    public void changePassword(String token, String newPassword) {
        if (token == null || !TOKEN_PATTERN.matcher(token).matches()) {
            throw invalidToken();
        }

        PasswordResetToken resetToken = resetTokens.findByTokenHash(hashes.hash(token))
                .orElseThrow(PasswordResetService::invalidToken);
        Long userId = resetToken.getUserId();
        UserCredentials accountCredentials = credentials.findForUpdate(userId)
                .orElseThrow(PasswordResetService::invalidToken);
        Instant now = clock.instant();
        if (resetToken.getUsedAt() != null || !resetToken.getExpiresAt().isAfter(now)
                || users.findById(userId).filter(User::isActive).isEmpty()) {
            throw invalidToken();
        }

        if (newPassword == null || !PASSWORD_PATTERN.matcher(newPassword).matches()) {
            throw new BusinessException(AuthErrorCode.PASSWORD_INVALID);
        }
        accountCredentials.changePassword(passwordEncoder.encode(newPassword), now);
        resetToken.markUsed(now);

        try {
            sessions.deleteByUserId(userId);
        } catch (DataAccessException exception) {
            throw new BusinessException(AuthErrorCode.SESSION_LOOKUP_UNAVAILABLE);
        }
    }

    private static BusinessException invalidToken() {
        return new BusinessException(AuthErrorCode.PASSWORD_RESET_TOKEN_INVALID);
    }
}
