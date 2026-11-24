package com.preen.preen_backend.auth.service;

import com.preen.preen_backend.auth.entity.User;
import com.preen.preen_backend.auth.entity.UserCredentials;
import com.preen.preen_backend.auth.entity.UserPersonalInfo;
import com.preen.preen_backend.auth.exception.AuthErrorCode;
import com.preen.preen_backend.auth.exception.LoginRejectedException;
import com.preen.preen_backend.auth.repository.UserCredentialsRepository;
import com.preen.preen_backend.auth.repository.UserPersonalInfoRepository;
import com.preen.preen_backend.auth.repository.UserRepository;
import com.preen.preen_backend.common.exception.BusinessException;
import com.preen.preen_backend.common.util.EmailUtil;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoginAccountService {

    private final AuthHashService hashes;
    private final UserPersonalInfoRepository personalInfo;
    private final UserRepository users;
    private final UserCredentialsRepository credentials;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    // 로그인할 계정을 찾고 비밀번호를 확인합니다.
    @Transactional(noRollbackFor = LoginRejectedException.class)
    public User authenticate(String email, String password) {
        String emailHash = hashes.hash(EmailUtil.normalize(email));
        Long userId = personalInfo.findByEmailHash(emailHash)
                .map(UserPersonalInfo::getUserId)
                .orElseThrow(LoginAccountService::invalidCredentials);
        User user = users.findById(userId)
                .filter(User::isActive)
                .orElseThrow(LoginAccountService::invalidCredentials);
        UserCredentials accountCredentials = credentials.findForUpdate(userId)
                .orElseThrow(LoginAccountService::invalidCredentials);
        Instant now = clock.instant();
        if (accountCredentials.isLoginLocked(now)) {
            throw new LoginRejectedException(AuthErrorCode.ACCOUNT_LOCKED);
        }
        if (accountCredentials.getLockedUntil() != null) {
            accountCredentials.clearLoginFailures();
        }

        if (!passwordEncoder.matches(password, accountCredentials.getPasswordHash())) {
            Instant failedAt = clock.instant();
            accountCredentials.recordLoginFailure(failedAt);
            AuthErrorCode error = accountCredentials.isLoginLocked(failedAt)
                    ? AuthErrorCode.ACCOUNT_LOCKED : AuthErrorCode.INVALID_CREDENTIALS;
            throw new LoginRejectedException(error);
        }

        accountCredentials.clearLoginFailures();
        return user;
    }

    // 계정 조회 실패는 같은 오류로 처리합니다.
    private static BusinessException invalidCredentials() {
        return new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
    }
}
