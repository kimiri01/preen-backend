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

    // 로그인할 계정을 찾고 비밀번호가 맞으면 이전 실패 기록을 초기화합니다.
    // 비밀번호 실패를 응답으로 보내더라도 DB에 기록한 횟수와 잠금 시각을 저장합니다.
    @Transactional(noRollbackFor = LoginRejectedException.class)
    public User authenticate(String email, String password) {
        // 회원가입 때와 같은 방식으로 이메일을 정리하고 조회용 해시를 만듭니다.
        String emailHash = hashes.hash(EmailUtil.normalize(email));
        // 이메일 해시가 일치하는 회원의 번호를 가져옵니다.
        Long userId = personalInfo.findByEmailHash(emailHash)
                .map(UserPersonalInfo::getUserId)
                .orElseThrow(LoginAccountService::invalidCredentials);
        // 활성 상태이고 삭제되지 않은 계정만 로그인할 수 있습니다.
        User user = users.findById(userId)
                .filter(User::isActive)
                .orElseThrow(LoginAccountService::invalidCredentials);
        // 같은 회원 번호로 비밀번호 해시와 로그인 잠금 정보를 가져옵니다.
        UserCredentials accountCredentials = credentials.findForUpdate(userId)
                .orElseThrow(LoginAccountService::invalidCredentials);
        // 잠금 중에는 비밀번호를 확인하지 않고, 잠금 시간이 지나면 다시 0회부터 시작합니다.
        Instant now = clock.instant();
        if (accountCredentials.isLoginLocked(now)) {
            throw new LoginRejectedException(AuthErrorCode.ACCOUNT_LOCKED);
        }
        if (accountCredentials.getLockedUntil() != null) {
            accountCredentials.clearLoginFailures();
        }

        // 입력한 비밀번호를 회원가입 때 저장한 해시와 비교합니다.
        if (!passwordEncoder.matches(password, accountCredentials.getPasswordHash())) {
            Instant failedAt = clock.instant();
            accountCredentials.recordLoginFailure(failedAt);
            AuthErrorCode error = accountCredentials.isLoginLocked(failedAt)
                    ? AuthErrorCode.ACCOUNT_LOCKED : AuthErrorCode.INVALID_CREDENTIALS;
            throw new LoginRejectedException(error);
        }

        // 같은 트랜잭션에서 실패 횟수를 0으로 되돌리고 잠금 기록을 지웁니다.
        accountCredentials.clearLoginFailures();
        return user;
    }

    // 계정 조회 실패는 같은 오류로 처리합니다.
    private static BusinessException invalidCredentials() {
        return new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
    }
}














